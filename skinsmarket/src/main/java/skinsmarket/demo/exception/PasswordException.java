package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando la contraseña no cumple los requisitos mínimos
 * o las contraseñas ingresadas no coinciden entre sí.
 *
 * Idéntica al PasswordException del TPO aprobado.
 * Devuelve HTTP 400 Bad Request con el mensaje correspondiente.
 *
 * Requisitos validados en AuthenticationService.register():
 *   - Mínimo 5 caracteres
 *   - Al menos una letra
 *   - Al menos un número
 *   - password y passwordRepeat deben coincidir
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "Las contraseñas deben incluir al menos una letra, un número, " +
                 "minimo 5 caracteres y deben coincidir.")
public class PasswordException extends Exception {
}
