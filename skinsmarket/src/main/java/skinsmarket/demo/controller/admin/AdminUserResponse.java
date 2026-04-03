package skinsmarket.demo.controller.admin;

import lombok.Data;
import skinsmarket.demo.entity.Role;

/**
 * DTO de respuesta para la vista de usuarios en el panel de administración.
 *
 * Expone los datos relevantes de cada usuario para que el administrador
 * pueda gestionar cuentas y roles sin acceder a información sensible
 * como contraseñas hasheadas.
 */
@Data
public class AdminUserResponse {

    // Identificador único del usuario en la base de datos
    private Long id;

    // Email del usuario (identificador principal en el sistema)
    private String email;

    // Nombre del usuario
    private String firstName;

    // Apellido del usuario
    private String lastName;

    // Rol actual del usuario en el sistema (USER o ADMIN)
    // Se usa el enum Role directamente para mantener consistencia con la entidad
    private Role role;
}