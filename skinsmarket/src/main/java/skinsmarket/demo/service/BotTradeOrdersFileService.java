package skinsmarket.demo.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service que gestiona el archivo orders.json compartido con el bot de Steam.
 *
 * ARQUITECTURA:
 *   - El backend Java ESCRIBE entradas cuando se crea una orden que necesita
 *     interacción del bot (PURCHASE pagada, SALE, EXCHANGE, RETURN).
 *   - El bot Node.js LEE el archivo periódicamente, procesa las órdenes
 *     pendientes y actualiza el estado en el mismo archivo.
 *   - El backend Java tiene un scheduler que RELEE el archivo cada 5s para
 *     sincronizar los cambios del bot a la BD.
 *
 * CONCURRENCIA:
 *   Usamos ReentrantReadWriteLock para evitar que dos threads escriban al
 *   mismo tiempo. NO previene race conditions con el bot Node.js (que es
 *   un proceso aparte) — para eso confiamos en escritura atómica con
 *   File.renameTo (write-then-rename pattern).
 *
 * CUANDO STEAM HABILITE EL TRADING (después del 10/06):
 *   El bot Node.js procesará estas entradas y mandará trade offers reales.
 *   Mientras tanto, el scheduler MockTradeScheduler dentro del Java simula
 *   las transiciones de estado.
 */
@Service
public class BotTradeOrdersFileService {

    @Value("${bot.orders.file.path:data/orders.json}")
    private String ordersFilePath;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    public void init() {
        File f = new File(ordersFilePath);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!f.exists()) {
            try {
                writeAll(new ArrayList<>());
                System.out.println("[BotOrdersFile] Archivo creado: " + f.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("[BotOrdersFile] No se pudo crear el archivo: " + e.getMessage());
            }
        } else {
            System.out.println("[BotOrdersFile] Usando archivo existente: " + f.getAbsolutePath());
        }
    }

    /**
     * Estructura de cada entrada del orders.json.
     * Es el "contrato" con el bot Node.js — el bot espera estos campos.
     */
    public static class BotOrder {
        public Long orderId;
        public String operationType;       // PURCHASE / SALE / EXCHANGE / RETURN
        public String status;              // PENDING / SENT / ACCEPTED / REJECTED / EXPIRED / CANCELLED
        public String direction;           // BOT_TO_USER / USER_TO_BOT
        public String partnerSteamId64;    // SteamID del USER
        public String partnerTradeUrl;     // tradeUrl del USER
        public List<String> assetIds;      // assetIds esperados
        public String botTradeOfferId;     // se llena cuando el bot envía la oferta
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public String mockMode;            // "true" si está siendo manejado por el mock interno

        public BotOrder() {}
    }

    /** Lee todas las entradas del archivo. */
    public List<BotOrder> readAll() {
        lock.readLock().lock();
        try {
            File f = new File(ordersFilePath);
            if (!f.exists() || f.length() == 0) {
                return new ArrayList<>();
            }
            BotOrder[] arr = mapper.readValue(f, BotOrder[].class);
            List<BotOrder> list = new ArrayList<>();
            for (BotOrder o : arr) list.add(o);
            return list;
        } catch (IOException e) {
            System.err.println("[BotOrdersFile] Error al leer: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sobreescribe el archivo completo con la lista dada.
     * Usa write-then-rename para evitar archivos corruptos si el proceso muere
     * en mitad de la escritura.
     */
    public void writeAll(List<BotOrder> orders) throws IOException {
        lock.writeLock().lock();
        try {
            File target = new File(ordersFilePath);
            File tmp = new File(ordersFilePath + ".tmp");
            mapper.writeValue(tmp, orders);
            if (target.exists()) target.delete();
            if (!tmp.renameTo(target)) {
                throw new IOException("No se pudo renombrar el archivo temporal");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Agrega una entrada nueva o actualiza la existente (por orderId).
     */
    public synchronized void upsert(BotOrder entry) {
        try {
            List<BotOrder> all = readAll();
            // Quitar previa si existe
            all.removeIf(o -> o.orderId != null && o.orderId.equals(entry.orderId));
            entry.updatedAt = LocalDateTime.now();
            if (entry.createdAt == null) entry.createdAt = entry.updatedAt;
            all.add(entry);
            writeAll(all);
            System.out.println("[BotOrdersFile] Upsert order " + entry.orderId +
                    " status=" + entry.status);
        } catch (IOException e) {
            System.err.println("[BotOrdersFile] Error en upsert: " + e.getMessage());
        }
    }

    /** Busca una entrada por orderId. */
    public Optional<BotOrder> findByOrderId(Long orderId) {
        return readAll().stream()
                .filter(o -> o.orderId != null && o.orderId.equals(orderId))
                .findFirst();
    }

    /** Marca una entrada con un estado específico (lo que normalmente haría el bot). */
    public void updateStatus(Long orderId, String newStatus) {
        Optional<BotOrder> opt = findByOrderId(orderId);
        if (opt.isPresent()) {
            BotOrder o = opt.get();
            o.status = newStatus;
            upsert(o);
        }
    }

    /** Elimina una entrada (cuando la operación termina, se puede limpiar). */
    public void delete(Long orderId) {
        try {
            List<BotOrder> all = readAll();
            all.removeIf(o -> o.orderId != null && o.orderId.equals(orderId));
            writeAll(all);
        } catch (IOException e) {
            System.err.println("[BotOrdersFile] Error en delete: " + e.getMessage());
        }
    }
}
