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

    // Username público del usuario (distinto del email usado para login)
    private String username;

    // Email del usuario (también sirve como identificador visible)
    private String email;

    // Indica si el email fue confirmado desde el link enviado al inbox
    private Boolean emailVerified;

    // Nombre del usuario
    private String firstName;

    // Apellido del usuario
    private String lastName;

    // SteamID64 (puede ser null si el usuario no lo configuró)
    private String steamId64;

    // Trade URL de Steam (puede ser null)
    private String tradeUrl;
}
