package skinsmarket.demo.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.service.UserService;
import skinsmarket.demo.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para las operaciones administrativas del marketplace de skins.
 *
 * Idéntico en estructura al AdminController del TPO aprobado.
 * Todas las rutas están protegidas y solo son accesibles por usuarios con rol ADMIN.
 *
 * Funcionalidades:
 *   - Gestión de roles de usuarios
 *   - Visualización de todos los usuarios registrados
 *   - Visualización de todas las órdenes de compra del sistema
 *
 * Ruta base: /api/v1/admin
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
// La anotación @PreAuthorize a nivel de clase aplica la restricción a TODOS los endpoints
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    // Inyección del servicio de usuarios via constructor (Lombok @RequiredArgsConstructor)
    private final UserService userService;

    // Inyección del servicio de órdenes para acceder a todas las compras del sistema
    private final OrderService orderService;

    // -------------------------------------------------------------------------
    // GESTIÓN DE USUARIOS
    // -------------------------------------------------------------------------

    /**
     * Cambia el rol de un usuario (USER ↔ ADMIN).
     * PUT /api/v1/admin/usuarios/{userId}/rol
     *
     * Solo accesible por ADMIN.
     * El nuevo rol se pasa en el body como JSON: { "nuevoRol": "ADMIN" }
     *
     * @param userId           ID del usuario cuyo rol se desea cambiar
     * @param changeRoleRequest objeto con el nuevo rol a asignar
     */
    @PutMapping("/usuarios/{userId}/rol")
    public ResponseEntity<String> cambiarRol(
            @PathVariable Long userId,
            @RequestBody ChangeRoleRequest changeRoleRequest) {

        // Convertimos a mayúsculas para asegurar consistencia con el enum Role
        userService.cambiarRolUser(userId, changeRoleRequest.getNuevoRol().toUpperCase());
        return ResponseEntity.ok("Rol actualizado correctamente");
    }

    /**
     * Devuelve la lista completa de usuarios registrados en el sistema.
     * GET /api/v1/admin/usuarios
     *
     * Solo accesible por ADMIN.
     * Útil para el panel de administración del marketplace.
     */
    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        List<AdminUserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // -------------------------------------------------------------------------
    // GESTIÓN DE ÓRDENES
    // -------------------------------------------------------------------------

    /**
     * Devuelve todas las órdenes de compra registradas en el sistema.
     * GET /api/v1/admin/orders
     *
     * Solo accesible por ADMIN.
     * Permite al administrador monitorear todas las transacciones del marketplace.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<AdminOrderResponse>> getAllOrders() {

        // 1. Obtenemos todas las entidades de órdenes desde el servicio
        List<Order> orders = orderService.findAllOrders();

        // 2. Mapeamos cada entidad a un DTO de respuesta para el admin
        List<AdminOrderResponse> response = orders.stream()
                .map(this::mapToAdminOrderResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // MÉTODO AUXILIAR DE MAPEO
    // -------------------------------------------------------------------------

    /**
     * Convierte una entidad Order al DTO AdminOrderResponse.
     *
     * Si el usuario asociado a la orden fue eliminado del sistema,
     * se muestra "Usuario Eliminado" en lugar del email.
     *
     * @param order entidad Order a convertir
     * @return AdminOrderResponse con los datos resumidos de la orden
     */
    private AdminOrderResponse mapToAdminOrderResponse(Order order) {

        // Protección ante usuarios eliminados: si el user es null, mostramos un texto descriptivo
        String userEmail = (order.getUser() != null)
                ? order.getUser().getEmail()
                : "Usuario Eliminado";

        return AdminOrderResponse.builder()
                .id(order.getId())
                .userEmail(userEmail)
                // Convertimos Double a BigDecimal para mayor precisión en montos monetarios
                .totalAmount(BigDecimal.valueOf(order.getTotalPrice()))
                .creationDate(order.getDate())
                .build();
    }
}