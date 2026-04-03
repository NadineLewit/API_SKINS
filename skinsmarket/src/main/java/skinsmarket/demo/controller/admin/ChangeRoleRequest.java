package skinsmarket.demo.controller.admin;

import lombok.Data;

/**
 * DTO para las solicitudes de cambio de rol de un usuario.
 *
 * Se utiliza en el endpoint PUT /api/v1/admin/usuarios/{userId}/rol
 * para indicar el nuevo rol que se le asignará al usuario.
 *
 * Valores válidos para nuevoRol: "USER" o "ADMIN"
 */
@Data
public class ChangeRoleRequest {

    // Nuevo rol a asignar al usuario
    // Se convierte a mayúsculas en el controlador antes de procesarse
    // Valores válidos: "USER" | "ADMIN"
    private String nuevoRol;
}
