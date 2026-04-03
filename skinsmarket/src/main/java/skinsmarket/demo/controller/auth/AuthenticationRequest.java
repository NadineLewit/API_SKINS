package skinsmarket.demo.controller.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para las solicitudes de autenticación (login).
 *
 * Contiene las credenciales del usuario para iniciar sesión.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    // Email del usuario (usado como identificador único en el sistema)
    private String email;

    // Contraseña en texto plano (se verifica contra el hash almacenado en BD)
    String password;
}