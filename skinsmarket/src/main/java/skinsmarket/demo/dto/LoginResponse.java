package skinsmarket.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Lo que devuelve el servidor al hacer login exitoso
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String rol;
}
