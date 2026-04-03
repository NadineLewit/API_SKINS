package skinsmarket.demo.controller.user;

import lombok.Data;

/**
 * DTO de respuesta con los datos del perfil del usuario.
 *
 * Expone solo los datos que el cliente necesita ver, sin exponer
 * información sensible como la contraseña hasheada o el rol interno.
 */
@Data
public class UserResponse {

    // Email del usuario (también sirve como identificador visible)
    private String email;

    // Nombre del usuario
    private String firstName;

    // Apellido del usuario
    private String lastName;
}