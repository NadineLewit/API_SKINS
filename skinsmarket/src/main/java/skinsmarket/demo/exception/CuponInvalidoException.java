package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "El cupón es inválido, está vencido o inactivo")
public class CuponInvalidoException extends RuntimeException {
    public CuponInvalidoException() {
        super("El cupón es inválido, está vencido o inactivo");
    }
}