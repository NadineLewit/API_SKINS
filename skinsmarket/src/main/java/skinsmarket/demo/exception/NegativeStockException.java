package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se intenta crear o editar una skin con stock negativo.
 *
 * Devuelve HTTP 400 Bad Request con el mensaje correspondiente.
 *
 * Lanzada en: SkinServiceImpl al validar el stock antes de persistir.
 * El stock puede ser 0 (agotado) pero no puede ser negativo.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "El campo stock debe ser un numero positvo o 0")
public class NegativeStockException extends Exception {
}
