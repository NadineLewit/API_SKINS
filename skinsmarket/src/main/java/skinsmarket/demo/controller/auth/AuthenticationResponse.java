package skinsmarket.demo.controller.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para el inicio de sesión (login).
 *
 * Solo devuelve el token para reconstruir el usuario y demas en el front
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    // Token JWT - usar como: Authorization: Bearer <token> en cada request
    @JsonProperty("access_token")
    private String accessToken;
}