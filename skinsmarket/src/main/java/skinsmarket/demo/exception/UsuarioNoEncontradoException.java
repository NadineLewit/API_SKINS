package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "Usuario no encontrado")
public class UsuarioNoEncontradoException extends RuntimeException {
  public UsuarioNoEncontradoException() {
    super("Usuario no encontrado");
  }
}