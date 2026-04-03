package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se intenta crear o editar una skin con precio negativo o cero.
 *
 * Devuelve HTTP 400 Bad Request con el mensaje correspondiente.
 *
 * Lanzada en: SkinServiceImpl al validar el precio antes de persistir.
 * El precio de una skin debe ser un número mayor a 0.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "El campo precio debe ser un numero mayor a 0.")
public class NegativePriceException extends Exception {
}
