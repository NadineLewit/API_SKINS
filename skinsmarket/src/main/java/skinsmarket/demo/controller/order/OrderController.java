package skinsmarket.demo.controller.order;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de Órdenes de compra.
 *
 * El flujo de la orden está envuelto en @Transactional en el service:
 * si algo falla, se hace rollback automático (stock vuelve, orden no se guarda).
 *
 * DECISIÓN DE DISEÑO:
 *   - La única forma de crear una orden es a través del carrito (POST /order/from-carrito).
 *     El endpoint anterior POST /order con itemList explícito fue eliminado porque
 *     no tiene sentido en el flujo del marketplace: el comprador siempre arma el
 *     carrito antes de confirmar la compra.
 *   - No se permite eliminar órdenes (DELETE /order/{id} fue eliminado). Una orden
 *     ya creada es un registro histórico de una transacción real y no debe borrarse.
 *
 * Todas las respuestas siguen el formato uniforme ApiResponse.
 */
@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Crea una orden a partir del carrito del usuario.
     * POST /order/from-carrito
     * POST /order/from-carrito?codigoCupon=PROMO2027
     * TOKEN: USER — sin body, lee el carrito automáticamente
     */
    @PostMapping("/from-carrito")
    public ResponseEntity<?> createOrderFromCarrito(
            Authentication auth,
            @RequestParam(required = false) String codigoCupon) {
        try {
            String email = auth.getName();
            OrderResponse result = orderService.createOrderFromCarrito(email, codigoCupon);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Orden creada desde el carrito", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /**
     * Historial de órdenes del usuario autenticado.
     * GET /order/me
     * TOKEN: USER
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyOrders(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        List<OrderResponse> orders = orderService.getOrdersForUser(user);
        return ResponseEntity.ok(
                ApiResponse.of("Tus órdenes (" + orders.size() + ")", orders));
    }

    /**
     * Obtiene una orden por ID.
     * GET /order/{id}
     * TOKEN: USER — solo devuelve órdenes propias
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(
            Authentication auth,
            @PathVariable Long id) {
        String email = auth.getName();
        OrderResponse order = orderService.getOrderById(id, email);
        if (order != null) {
            return ResponseEntity.ok(ApiResponse.of("Orden encontrada", order));
        }
        return ResponseEntity.status(404)
                .body(ApiResponse.of("Orden no encontrada con id: " + id));
    }
}
