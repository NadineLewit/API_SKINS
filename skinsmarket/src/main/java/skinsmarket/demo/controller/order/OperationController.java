package skinsmarket.demo.controller.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.service.TradeOperationService;

import java.util.List;

/**
 * Controlador para operaciones que involucran al bot de Steam:
 *   - Venta (USER → BOT)
 *   - Intercambio
 *   - Cancelación con eventual devolución automática
 *   - Consulta de estado
 *
 * El flujo de COMPRA clásico (POST /order/from-carrito → /payments/...) NO se
 * tocó: sigue siendo independiente. Solo se le agregó automáticamente el
 * tradeStatus para que el mock del bot lo procese.
 */
@RestController
@RequestMapping("operations")
public class OperationController {

    @Autowired
    private TradeOperationService tradeService;

    /** POST /operations/sale — crear orden de venta. */
    @PostMapping("/sale")
    public ResponseEntity<?> createSale(Authentication auth, @RequestBody SaleRequest request) {
        try {
            OperationStatusResponse r = tradeService.createSale(auth.getName(), request);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Orden de venta creada. Enviá la oferta al bot en Steam.", r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /** POST /operations/exchange — crear orden de intercambio. */
    @PostMapping("/exchange")
    public ResponseEntity<?> createExchange(Authentication auth, @RequestBody ExchangeRequest request) {
        try {
            OperationStatusResponse r = tradeService.createExchange(auth.getName(), request);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Orden de intercambio creada.", r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /**
     * POST /operations/{id}/cancel — cancela una operación.
     * Si el USER ya entregó skins, genera automáticamente una devolución.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(Authentication auth, @PathVariable Long id) {
        try {
            OperationStatusResponse r = tradeService.cancelOperation(auth.getName(), id);
            return ResponseEntity.ok(ApiResponse.of("Operación cancelada", r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /**
     * GET /operations/{id}/status — consulta estado completo de una operación.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<?> status(Authentication auth, @PathVariable Long id) {
        try {
            OperationStatusResponse r = tradeService.getStatus(auth.getName(), id);
            return ResponseEntity.ok(ApiResponse.of("Estado de la operación", r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /** GET /operations/me — todas las operaciones del USER. */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        List<OperationStatusResponse> list = tradeService.listMyOperations(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.of("Mis operaciones (" + list.size() + ")", list));
    }

    // =========================================================================
    // Endpoints para el bot / admin / mock
    // =========================================================================

    /**
     * POST /operations/{id}/user-trade-received — el bot confirma que recibió
     * las skins del USER. En modo mock real, esto lo hace el scheduler
     * automáticamente; pero dejamos el endpoint disponible para testing manual.
     *
     * En el futuro (post 10/06) el bot real va a llamar este endpoint cuando
     * detecte que la oferta entrante coincidió con los assetIds esperados.
     */
    @PostMapping("/{id}/user-trade-received")
    public ResponseEntity<?> userTradeReceived(@PathVariable Long id) {
        try {
            OperationStatusResponse r = tradeService.markUserTradeReceived(id);
            return ResponseEntity.ok(ApiResponse.of(
                    "Trade del usuario confirmado por el bot", r));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }
}
