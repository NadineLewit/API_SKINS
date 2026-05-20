package skinsmarket.demo.controller.auth;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String password;
    private String passwordRepeat;
}
