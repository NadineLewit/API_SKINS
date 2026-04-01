package skinsmarket.demo.dto;

import lombok.Data;
import skinsmarket.demo.entity.Usuario;

import java.time.LocalDateTime;

@Data
public class UsuarioResponse {

    private Long id;
    private String username;
    private String email;
    private String nombre;
    private String apellido;
    private String rol;
    private LocalDateTime fechaRegistro;

    public static UsuarioResponse fromEntity(Usuario u) {
        UsuarioResponse dto = new UsuarioResponse();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setEmail(u.getEmail());
        dto.setNombre(u.getNombre());
        dto.setApellido(u.getApellido());
        dto.setRol(u.getRol().name());
        dto.setFechaRegistro(u.getFechaRegistro());
        return dto;
    }
}
