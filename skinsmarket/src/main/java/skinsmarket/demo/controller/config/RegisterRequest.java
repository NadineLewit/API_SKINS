package skinsmarket.demo.controller.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para las solicitudes de registro de nuevos usuarios.
 *
 *
 * NOTA: La autenticación (login) se realiza por EMAIL aunque se pida
 * username en el registro, para simplificar el flujo del frontend.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    // Nombre de usuario único requerido en el formulario de registro.
    private String username;

    // Nombre del usuario.
    private String firstName;

    // Apellido del usuario.
    private String lastName;

    // Email único del usuario, usado como identificador para el login
    private String email;

    // Contraseña elegida por el usuario (se almacenará hasheada con BCrypt)
    private String password;

    // Repetición de la contraseña para confirmar que no hay errores de tipeo
    private String passwordRepeat;
}
