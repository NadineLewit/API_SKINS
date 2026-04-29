package skinsmarket.demo.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.service.CuponService;
import skinsmarket.demo.service.UserService;
import skinsmarket.demo.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para las operaciones administrativas del marketplace de skins.
 *
 * Todas las rutas están protegidas y solo son accesibles por usuarios con rol ADMIN.
 *
 * Funcionalidades:
 *   - Gestión de roles de usuarios
 *   - Visualización de todos los usuarios registrados
 *   - Visualización de todas las órdenes de compra del sistema
 *   - Búsqueda de cupones por ID
 *
 * Ruta base: /api/v1/admin
 *
 * Todas las respuestas siguen el formato uniforme ApiResponse.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final OrderService orderService;
    private final CuponService cuponService;

    // -------------------------------------------------------------------------
    // GESTIÓN DE USUARIOS
    // -------------------------------------------------------------------------

    /**
     * Cambia el rol de un usuario (USER ↔ ADMIN).
     * PUT /api/v1/admin/usuarios/{userId}/rol
     *
     * El nuevo rol se pasa en el body como JSON: { "nuevoRol": "ADMIN" }
     */
    @PutMapping("/usuarios/{userId}/rol")
    public ResponseEntity<?> cambiarRol(
            @PathVariable Long userId,
            @RequestBody ChangeRoleRequest changeRoleRequest) {

        userService.cambiarRolUser(userId, changeRoleRequest.getNuevoRol().toUpperCase());
        return ResponseEntity.ok(ApiResponse.of(
                "Rol actualizado correctamente a " + changeRoleRequest.getNuevoRol().toUpperCase()));
    }

    /**
     * Devuelve la lista completa de usuarios registrados en el sistema.
     * GET /api/v1/admin/usuarios
     */
    @GetMapping("/usuarios")
    public ResponseEntity<?> getAllUsers() {
        List<AdminUserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(
                ApiResponse.of("Listado de usuarios (" + users.size() + ")", users));
    }

    // -------------------------------------------------------------------------
    // GESTIÓN DE CUPONES
    // -------------------------------------------------------------------------

    /**
     * Busca un cupón específico por su ID.
     * GET /api/v1/admin/cupones/{id}
     */
    @GetMapping("/cupones/{id}")
    public ResponseEntity<?> getCuponById(@PathVariable Long id) {
        try {
            Cupon cupon = cuponService.obtenerPorId(id);
            return ResponseEntity.ok(ApiResponse.of("Cupón encontrado", cupon));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Cupón no encontrado con id: " + id));
        }
    }

    // -------------------------------------------------------------------------
    // GESTIÓN DE ÓRDENES
    // -------------------------------------------------------------------------

    /**
     * Devuelve todas las órdenes de compra registradas en el sistema.
     * GET /api/v1/admin/orders
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderService.findAllOrders();

        List<AdminOrderResponse> response = orders.stream()
                .map(this::mapToAdminOrderResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.of("Listado de órdenes del sistema (" + response.size() + ")", response));
    }

    // -------------------------------------------------------------------------
    // MÉTODO AUXILIAR DE MAPEO
    // -------------------------------------------------------------------------

    /**
     * Convierte una entidad Order al DTO AdminOrderResponse.
     *
     * Si el usuario asociado a la orden fue eliminado del sistema,
     * se muestra "Usuario Eliminado" en lugar del email.
     */
    private AdminOrderResponse mapToAdminOrderResponse(Order order) {
        String userEmail = (order.getUser() != null)
                ? order.getUser().getEmail()
                : "Usuario Eliminado";

        return AdminOrderResponse.builder()
                .id(order.getId())
                .userEmail(userEmail)
                .totalAmount(BigDecimal.valueOf(order.getTotalPrice()))
                .creationDate(order.getDate())
                .build();
    }
}
