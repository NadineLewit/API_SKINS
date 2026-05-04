package skinsmarket.demo.controller.inventario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.service.InventarioService;

import java.util.List;

/**
 * Controlador REST para el inventario de Steam de cada usuario.
 *
 * Todas las rutas requieren autenticación (USER o ADMIN).
 * El usuario debe tener configurado su steamId64 (vía PUT /api/v1/users/me)
 * antes de poder usar estos endpoints.
 *
 * Flujo típico de uso:
 *   1. PUT /api/v1/users/me con {"steamId64": "76561198..."} → setea el SteamID
 *      (esto dispara una sincronización automática inicial)
 *   2. GET /inventario → lista los items que se sincronizaron
 *   3. POST /inventario/sync → forzar resync manual cuando quiera
 *   4. POST /inventario/{itemId}/publicar → publicar uno a la venta con 1 click
 */
@RestController
@RequestMapping("inventario")
public class InventarioController {

    @Autowired
    private InventarioService inventarioService;

    /**
     * Lista los items del inventario del usuario autenticado.
     * GET /inventario
     */
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
     * Re-sincroniza el inventario contra Steam (manual).
     * POST /inventario/sync
     *
     * Útil cuando el usuario tradeó un item afuera de la app y quiere que
     * el marketplace refleje el estado real de su inventario de Steam.
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
     * Publica un item del inventario a la venta en el marketplace.
     * POST /inventario/{itemId}/publicar
     *
     * Body: { "price": 25.50, "discount": 0.0 }
     *
     * El item debe:
     *   - Pertenecer al usuario autenticado
     *   - No estar ya publicado
     *   - Tener match con el catálogo (sincronizá el catálogo si te falta)
     */
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
