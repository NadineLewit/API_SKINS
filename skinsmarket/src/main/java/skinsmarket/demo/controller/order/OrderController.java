package skinsmarket.demo.controller.order;

import skinsmarket.demo.entity.User;
import skinsmarket.demo.exception.NoStockAvailableException;
import skinsmarket.demo.exception.PropietarioSkinException;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    // =========================================================================
    // Crear orden con itemList explícito
    // POST /order
    // TOKEN: USER
    // =========================================================================
    @PostMapping
    public ResponseEntity<Object> createOrder(
            Authentication auth,
            @RequestBody OrderRequest orderRequest)
            throws NoStockAvailableException, PropietarioSkinException {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        OrderResponse result = orderService.createOrder(user, orderRequest);
        return ResponseEntity.ok(result);
    }

    // =========================================================================
    // Crear orden desde el carrito (nuevo)
    // POST /order/from-carrito
    // POST /order/from-carrito?codigoCupon=PROMO2027
    // TOKEN: USER — sin body, lee el carrito automáticamente
    // =========================================================================
    @PostMapping("/from-carrito")
    public ResponseEntity<?> createOrderFromCarrito(
            Authentication auth,
            @RequestParam(required = false) String codigoCupon) {
        try {
            String email = auth.getName();
            OrderResponse result = orderService.createOrderFromCarrito(email, codigoCupon);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // Historial de órdenes del usuario
    // GET /order/me
    // TOKEN: USER
    // =========================================================================
    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        return ResponseEntity.ok(orderService.getOrdersForUser(user));
    }

    // =========================================================================
    // Obtener orden por ID
    // GET /order/{id}
    // TOKEN: USER — solo devuelve órdenes propias
    // =========================================================================
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            Authentication auth,
            @PathVariable Long id) {
        String email = auth.getName();
        OrderResponse order = orderService.getOrderById(id, email);
        if (order != null) return ResponseEntity.ok(order);
        return ResponseEntity.notFound().build();
    }

    // =========================================================================
    // Eliminar orden
    // DELETE /order/{id}
    // TOKEN: USER — solo puede eliminar sus propias órdenes
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrder(
            Authentication auth,
            @PathVariable Long id) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
        try {
            orderService.deleteOrder(id, user);
            return ResponseEntity.ok("Orden eliminada exitosamente");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
