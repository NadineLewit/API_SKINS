package skinsmarket.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.OperationType;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.OrderDetail;
import skinsmarket.demo.entity.TradeStatus;
import skinsmarket.demo.repository.InventarioItemRepository;
import skinsmarket.demo.repository.OrderRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Scheduler que SIMULA al bot de Steam mientras la cuenta real está bloqueada
 * hasta el 10/06.
 *
 * CORRE CADA 5 SEGUNDOS y procesa las órdenes según su estado:
 *
 *   1. PURCHASE + PAID + WAITING_PAYMENT → PREPARING_TRADE
 *      (el pago llegó por MP, el bot prepara la entrega)
 *
 *   2. PREPARING_TRADE → BOT_SENT
 *      (el bot envía la oferta al usuario)
 *
 *   3. BOT_SENT (más de 5s en este estado) → COMPLETED
 *      (simulamos que el usuario aceptó el trade)
 *
 *   4. SALE/EXCHANGE en WAITING_USER_TRADE (más de 10s) → USER_TRADE_RECEIVED
 *      (simulamos que el usuario envió las skins al bot)
 *
 *   5. RETURN_PENDING → RETURN_SENT → RETURNED (devolución completa)
 *
 * CUANDO STEAM HABILITE EL TRADING (10/06):
 *   Apagar el mock con `mock.enabled=false` en application.properties.
 *   El bot Node.js real va a hacer todas estas transiciones leyendo el
 *   archivo orders.json y actualizándolo. El backend Java va a leer ese
 *   archivo (BotTradeOrdersFileService) y sincronizar los estados a la BD.
 */
@Component
public class MockTradeScheduler {

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    /** Segundos que tarda el "bot" en aceptar el trade enviado. */
    @Value("${mock.bot.delay-seconds:5}")
    private long mockDelaySeconds;

    @Autowired private OrderRepository orderRepository;
    @Autowired private InventarioItemRepository inventarioItemRepository;
    @Autowired private BotTradeOrdersFileService botFileService;

    /**
     * Tick cada 5 segundos. Procesa todas las órdenes pendientes.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void tick() {
        if (!mockEnabled) return;

        List<Order> pendientes = orderRepository.findByTradeStatusIn(List.of(
                TradeStatus.WAITING_PAYMENT,
                TradeStatus.WAITING_USER_TRADE,
                TradeStatus.PREPARING_TRADE,
                TradeStatus.BOT_SENT,
                TradeStatus.RETURN_PENDING,
                TradeStatus.RETURN_SENT
        ));

        if (pendientes.isEmpty()) return;

        System.out.println("[MOCK] Procesando " + pendientes.size() + " órdenes pendientes...");

        for (Order order : pendientes) {
            try {
                procesar(order);
            } catch (Exception e) {
                System.err.println("[MOCK] Error procesando orden " + order.getId() +
                        ": " + e.getMessage());
            }
        }
    }

    private void procesar(Order order) {
        TradeStatus current = order.getTradeStatus();
        long secondsSinceUpdate = secondsSince(order.getDate());

        // CASO 1: Compra pagada → preparar trade
        if (order.getOperationType() == OperationType.PURCHASE &&
            current == TradeStatus.WAITING_PAYMENT &&
            "PAID".equals(order.getPaymentStatus())) {

            avanzarA(order, TradeStatus.PREPARING_TRADE,
                    "Compra pagada, preparando envío del bot");
            return;
        }

        // CASO 2: SALE/EXCHANGE esperando trade del USER (más de 10s) → recibido
        if (current == TradeStatus.WAITING_USER_TRADE && secondsSinceUpdate >= 10) {
            if (order.getOperationType() == OperationType.SALE) {
                avanzarA(order, TradeStatus.PREPARING_TRADE,
                        "Mock: USER habría enviado skins, bot recibió, preparando pago");
                // Marcar items del USER como "publicados" para que no los use de nuevo
                marcarItemsComoBloqueados(order);
            } else if (order.getOperationType() == OperationType.EXCHANGE) {
                TradeStatus next = (order.getPriceDifference() != null && order.getPriceDifference() > 0)
                        ? TradeStatus.WAITING_DIFFERENCE
                        : TradeStatus.PREPARING_TRADE;
                avanzarA(order, next,
                        "Mock: USER habría enviado skins, bot las recibió");
                marcarItemsComoBloqueados(order);
            }
            return;
        }

        // CASO 3: PREPARING_TRADE → BOT_SENT
        if (current == TradeStatus.PREPARING_TRADE && secondsSinceUpdate >= mockDelaySeconds) {
            order.setBotTradeOfferId("MOCK-" + UUID.randomUUID().toString().substring(0, 8));
            avanzarA(order, TradeStatus.BOT_SENT,
                    "Mock: bot envió la oferta al usuario, esperando aceptación");
            return;
        }

        // CASO 4: BOT_SENT → COMPLETED
        if (current == TradeStatus.BOT_SENT && secondsSinceUpdate >= mockDelaySeconds) {
            avanzarA(order, TradeStatus.COMPLETED,
                    "Mock: usuario aceptó la oferta, operación completada");
            return;
        }

        // CASO 5: RETURN_PENDING → RETURN_SENT
        if (current == TradeStatus.RETURN_PENDING && secondsSinceUpdate >= mockDelaySeconds) {
            order.setBotTradeOfferId("MOCK-RETURN-" + UUID.randomUUID().toString().substring(0, 8));
            avanzarA(order, TradeStatus.RETURN_SENT,
                    "Mock: bot envió la oferta de devolución");
            return;
        }

        // CASO 6: RETURN_SENT → RETURNED
        if (current == TradeStatus.RETURN_SENT && secondsSinceUpdate >= mockDelaySeconds) {
            avanzarA(order, TradeStatus.RETURNED,
                    "Mock: usuario aceptó la devolución, skins regresadas");
            return;
        }
    }

    private void avanzarA(Order order, TradeStatus next, String mensaje) {
        TradeStatus prev = order.getTradeStatus();
        order.setTradeStatus(next);
        orderRepository.save(order);
        System.out.println("[MOCK] Orden " + order.getId() + ": " + prev + " → " + next +
                " (" + mensaje + ")");
        // Reflejar en el archivo del bot
        botFileService.updateStatus(order.getId(), next.name());
    }

    private void marcarItemsComoBloqueados(Order order) {
        if (order.getUserSkinAssetIds() == null) return;
        // Parsear assetIds y marcar publicado=true en inventario para evitar reuso
        // (no es estrictamente necesario porque findActiveOrdersWithAssetId ya bloquea,
        // pero ayuda a la consistencia visual del inventario)
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> ids = m.readValue(order.getUserSkinAssetIds(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            for (String assetId : ids) {
                inventarioItemRepository.findByUserAndAssetId(order.getUser(), assetId)
                        .ifPresent(item -> {
                            item.setPublicado(true);
                            inventarioItemRepository.save(item);
                        });
            }
        } catch (Exception e) {
            System.err.println("[MOCK] No se pudieron bloquear items: " + e.getMessage());
        }
    }

    private long secondsSince(LocalDateTime when) {
        if (when == null) return Long.MAX_VALUE;
        return ChronoUnit.SECONDS.between(when, LocalDateTime.now());
    }
}
