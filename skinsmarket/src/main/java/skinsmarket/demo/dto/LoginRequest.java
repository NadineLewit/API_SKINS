package skinsmarket.demo.dto;

import lombok.Data;

// DTO para hacer login
@Data
public class LoginRequest {
    private String username;
    private String password;
}
