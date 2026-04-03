package skinsmarket.demo.controller.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta específico para el registro de nuevos usuarios.
 *
 * Se diferencia de AuthenticationResponse (login) en que incluye los datos
 * básicos del usuario recién creado, para que el frontend no tenga que hacer
 * un segundo request a GET /api/v1/users/me justo después de registrarse.
 *
 * NO incluye el rol: el frontend no necesita saber el rol en el registro
 * (todos se registran como USER por defecto) y exponerlo complica innecesariamente
 * la lógica del front.
 *
 * Devuelto por: POST /api/v1/auth/register
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistroResponse {

    // Token JWT para que el usuario pueda operar de inmediato tras registrarse
    @JsonProperty("access_token")
    private String accessToken;

    // Nombre de usuario registrado (el frontend lo puede mostrar en la UI)
    private String username;

    // Email del usuario recién registrado
    private String email;

    // Nombre del usuario (para el saludo de bienvenida en el frontend)
    private String firstName;

    // Apellido del usuario
    private String lastName;
}