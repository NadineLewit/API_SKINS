package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando el descuento de una skin no está en el rango válido.
 *
 * Devuelve HTTP 400 Bad Request con el mensaje correspondiente.
 *
 * Lanzada en: SkinServiceImpl al validar el descuento antes de persistir.
 * El valor de discount debe estar entre 0.0 y 1.0 (representando 0% a 100%).
 * Ejemplos válidos: 0.0, 0.15, 0.5, 1.0
 * Ejemplos inválidos: -0.1, 1.5, 2.0
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "El campo discount debe ser un numero entre 0 y 1.")
public class InvalidDiscountException extends Exception {
}
