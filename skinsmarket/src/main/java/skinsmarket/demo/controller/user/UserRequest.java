package skinsmarket.demo.controller.user;

import lombok.Data;

/**
 * DTO para actualización de perfil.
 * Todos los campos son opcionales — solo se actualizan los no nulos.
 */
@Data
public class UserRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    /**
     * Nuevo username. Si se envía distinto al actual, se valida la restricción
     * de 15 días desde el último cambio antes de permitir el cambio.
     */
    private String username;
}
