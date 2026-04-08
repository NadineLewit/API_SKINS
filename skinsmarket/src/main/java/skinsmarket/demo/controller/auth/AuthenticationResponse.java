package skinsmarket.demo.controller.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para login Y registro.
 *
 * Solo devuelve el token JWT. El frontend puede decodificar el payload
 * del token para obtener email, rol y cualquier otro claim sin hacer
 * requests adicionales.
 *
 * Usado en: POST /api/v1/auth/authenticate y POST /api/v1/auth/register
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    @JsonProperty("access_token")
    private String accessToken;
}
