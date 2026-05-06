package skinsmarket.demo.controller.inventario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.service.InventarioService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el inventario de Steam de cada usuario.
 *
 * Flujo típico:
 *   1. PUT /api/v1/users/me con steamId64 → sync auto en background
 *   2. POST /inventario/sync → resync manual (DEVUELVE LA RESPUESTA DE STEAM)
 *   3. GET /inventario → ver items sincronizados
 *   4. GET /inventario/debug → diagnóstico publicabilidad por item
 *   5. GET /inventario/status → diagnóstico del USER (steamId seteado, etc)
 *   6. POST /inventario/{id}/publicar → publicar a venta
 */
@RestController
@RequestMapping("inventario")
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /inventario/status — diagnóstico del USER autenticado.
     *
     * Endpoint de DEBUG para entender por qué el inventario está vacío.
     * Devuelve si el user tiene steamId configurado, cuántos items locales
     * tiene en BD, y cuándo fue el último sync.
     *
     * No llama a Steam — solo lee la BD.
     */
    @GetMapping("/status")
    public ResponseEntity<?> status(Authentication auth) {
        try {
            String email = auth.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

            List<InventarioItem> items = inventarioService.listarInventario(email);

            Map<String, Object> info = new HashMap<>();
            info.put("emailDelToken", email);
            info.put("userId", user.getId());
            info.put("username", user.getUsername());
            info.put("steamId64", user.getSteamId64());
            info.put("tieneSteamIdConfigurado", user.getSteamId64() != null
                    && !user.getSteamId64().isBlank());
            info.put("tradeUrl", user.getTradeUrl());
            info.put("itemsEnBD", items.size());
            info.put("ultimoSync", items.isEmpty() ? null : items.get(0).getFechaSync());

            return ResponseEntity.ok(ApiResponse.of("Estado del inventario", info));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /** Lista los items del inventario del usuario autenticado. */
    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        try {
            List<InventarioItem> items = inventarioService.listarInventario(auth.getName());
            return ResponseEntity.ok(
                    ApiResponse.of("Inventario del usuario (" + items.size() + " items)", items));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /**
     * Re-sincroniza el inventario contra Steam.
     *
     * IMPORTANTE: este endpoint llama a Steam. Si tu inventario es público y
     * el SteamID es válido, deberías ver tus items aparecer en BD.
     *
     * Si devuelve "Items totales: 0" pero tu Steam tiene items, hay un
     * problema más profundo (Steam te está bloqueando, o tu IP tiene
     * rate-limit silencioso).
     */
    @PostMapping("/sync")
    public ResponseEntity<?> sincronizar(Authentication auth) {
        try {
            int total = inventarioService.sincronizar(auth.getName());
            return ResponseEntity.ok(ApiResponse.of(
                    "Inventario sincronizado correctamente. Items totales: " + total));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /**
     * GET /inventario/debug — diagnóstico publicabilidad por item.
     */
    @GetMapping("/debug")
    public ResponseEntity<?> debug(Authentication auth) {
        try {
            List<InventarioItem> items = inventarioService.listarInventario(auth.getName());

            int total = items.size();
            int conCatalogo = 0;
            int sinCatalogo = 0;
            int conTradeLock = 0;
            int publicables = 0;
            int yaPublicados = 0;

            List<Map<String, Object>> detalle = new ArrayList<>();

            for (InventarioItem item : items) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", item.getId());
                info.put("name", item.getName());
                info.put("marketHashName", item.getMarketHashName());
                info.put("type", item.getType());
                info.put("tradable", item.getTradable());
                info.put("marketable", item.getMarketable());
                info.put("publicado", item.getPublicado());
                info.put("iconUrl", item.getIconUrl());

                boolean tieneCatalogo = item.getCatalogo() != null;
                boolean esTradeable = Boolean.TRUE.equals(item.getTradable());

                if (tieneCatalogo) {
                    conCatalogo++;
                    info.put("catalogoId", item.getCatalogo().getId());
                    info.put("catalogoName", item.getCatalogo().getName());
                } else {
                    sinCatalogo++;
                    info.put("catalogoId", null);
                }

                if (!esTradeable) conTradeLock++;
                if (Boolean.TRUE.equals(item.getPublicado())) yaPublicados++;

                List<String> bloqueos = new ArrayList<>();
                if (!tieneCatalogo) bloqueos.add("sin match en el catálogo");
                if (!esTradeable) bloqueos.add("trade lock activo o intransferible");
                if (Boolean.TRUE.equals(item.getPublicado())) bloqueos.add("ya publicado");

                if (bloqueos.isEmpty()) {
                    info.put("publicable", true);
                    info.put("razonBloqueo", null);
                    publicables++;
                } else {
                    info.put("publicable", false);
                    info.put("razonBloqueo", String.join(" + ", bloqueos));
                }

                detalle.add(info);
            }

            Map<String, Object> resumen = new HashMap<>();
            resumen.put("total", total);
            resumen.put("publicables", publicables);
            resumen.put("conCatalogo", conCatalogo);
            resumen.put("sinCatalogo_NoPublicables", sinCatalogo);
            resumen.put("conTradeLock", conTradeLock);
            resumen.put("yaPublicados", yaPublicados);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("resumen", resumen);
            respuesta.put("items", detalle);

            return ResponseEntity.ok(ApiResponse.of(
                    "Diagnóstico del inventario", respuesta));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /** Publica un item del inventario a la venta. */
    @PostMapping("/{itemId}/publicar")
    public ResponseEntity<?> publicar(
            Authentication auth,
            @PathVariable Long itemId,
            @RequestBody PublicarDesdeInventarioRequest request) {
        try {
            Skin skin = inventarioService.publicarDesdeInventario(
                    auth.getName(), itemId, request);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Item publicado a la venta exitosamente", skin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }
}
