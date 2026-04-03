package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando el email ingresado no tiene un formato válido
 * o ya está en uso por otro usuario.
 *
 * Idéntica al EmailException del TPO aprobado.
 * Devuelve HTTP 400 Bad Request con el mensaje correspondiente.
 *
 * Lanzada en:
 *   - AuthenticationService.register()  → email ya registrado
 *   - UserServiceImpl.actualizarUser()  → nuevo email ya en uso
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "El campo email debe tener un formato válido (name@example.com).")
public class EmailException extends Exception {
}
