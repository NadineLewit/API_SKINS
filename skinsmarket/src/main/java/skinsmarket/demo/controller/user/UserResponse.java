package skinsmarket.demo.controller.user;

import lombok.Data;
import skinsmarket.demo.entity.Role;

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

    // Saldo a favor generado por intercambios donde el usuario entrega más valor
    private Double saldo;

    // Cotización usada para mostrar el saldo interno en pesos argentinos
    private Double usdToArs;

    // Rol actual. El front lo usa para mostrar herramientas admin de desarrollo.
    private Role role;

    // SteamID64 (puede ser null si el usuario no lo configuró)
    private String steamId64;

    // Datos públicos obtenidos desde el perfil de Steam
    private String steamUsername;
    private String steamAvatarUrl;

    // Trade URL de Steam (puede ser null)
    private String tradeUrl;

    // Alias de cobro para recibir pagos por ventas (puede ser null)
    private String aliasCobro;
}
