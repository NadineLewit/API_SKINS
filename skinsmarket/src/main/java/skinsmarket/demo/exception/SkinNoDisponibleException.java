package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "La skin no está disponible o no existe")
public class SkinNoDisponibleException extends RuntimeException {
    public SkinNoDisponibleException() {
        super("La skin no está disponible o no existe");
    }
}