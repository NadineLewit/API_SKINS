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

/**
 * Controlador REST para la gestión de Órdenes de compra de skins.
 *
 *
 * Todas las rutas requieren autenticación (rol USER).
 * Ruta base: /order
 */
@RestController
@RequestMapping("order")
public class OrderController {

    // Inyección del servicio de órdenes
    @Autowired
    private OrderService orderService;

    // Inyección del repositorio de usuarios para obtener el usuario autenticado
    @Autowired
    private UserRepository userRepository;

    /**
     * Crea una nueva orden de compra a partir del carrito del usuario.
     * POST /order
     *
     * Opcionalmente acepta un código de cupón de descuento.
     * Valida el stock disponible de cada skin antes de confirmar la compra.
     *
     * @param orderRequest objeto con los items de la orden y opcional: código de cupón
     * @throws NoStockAvailableException si alguna skin no tiene stock suficiente
     */
    @PostMapping
    public ResponseEntity<Object> createOrder(
            Authentication auth,
            @RequestBody OrderRequest orderRequest)
            throws NoStockAvailableException, PropietarioSkinException {

        // Obtenemos el email del usuario autenticado desde el token JWT
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        // Delegamos la creación de la orden al servicio (incluye validación de cupón si viene)
        OrderResponse result = orderService.createOrder(user, orderRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Devuelve todas las órdenes del usuario autenticado.
     * GET /order/me
     *
     * Permite al usuario ver su historial de compras de skins.
     */
    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));

        List<OrderResponse> orders = orderService.getOrdersForUser(user);
        return ResponseEntity.ok(orders);
    }

    /**
     * Devuelve una orden específica por su ID.
     * GET /order/{id}
     *
     * El usuario solo puede ver sus propias órdenes (validación en la capa de servicio).
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            Authentication auth,
            @PathVariable Long id) {

        String email = auth.getName();
        OrderResponse order = orderService.getOrderById(id, email);
        if (order != null) {
            return ResponseEntity.ok(order);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}