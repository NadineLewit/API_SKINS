package skinsmarket.demo.controller.user;

import lombok.Data;

/**
 * DTO para las solicitudes de actualización de perfil de usuario.
 *
 * Todos los campos son opcionales en la request: solo se actualizan
 * los que no sean null (la validación se realiza en la capa de servicio).
 */
@Data
public class UserRequest {

    // Nuevo email del usuario (null si no se desea cambiar)
    private String email;

    // Nueva contraseña en texto plano (null si no se desea cambiar)
    // Si se envía, se hasheará con BCrypt antes de almacenar
    private String password;

    // Nuevo nombre del usuario (null si no se desea cambiar)
    private String firstName;

    // Nuevo apellido del usuario (null si no se desea cambiar)
    private String lastName;
}