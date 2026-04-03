package skinsmarket.demo.controller.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para el inicio de sesión (login).
 *
 * Actualizado: además del token JWT devuelve email y nombre del usuario
 * para que el frontend pueda mostrar quién está logueado sin un segundo request.
 *
 * NO incluye el rol: se guarda en el JWT como claim interno y no debe
 * exponerse en el body para no complicar el frontend.
 * La autenticación se realiza por EMAIL (no por nombre/username).
 *
 * Devuelto por: POST /api/v1/auth/authenticate
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    // Token JWT - usar como: Authorization: Bearer <token> en cada request
    @JsonProperty("access_token")
    private String accessToken;

    // Email del usuario autenticado (identifica la sesión en el frontend)
    private String email;

    // Nombre del usuario para mostrar en la UI sin un request extra
    private String firstName;
}