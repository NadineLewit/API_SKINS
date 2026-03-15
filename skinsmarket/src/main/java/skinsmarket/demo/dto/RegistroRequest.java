package skinsmarket.demo.dto;

import lombok.Data;

// DTO para registrar un usuario nuevo
@Data
public class RegistroRequest {
    private String username;
    private String email;
    private String password;
    private String nombre;
    private String apellido;
}
