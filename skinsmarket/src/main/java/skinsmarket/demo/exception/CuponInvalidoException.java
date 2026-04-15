package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un cupón de descuento no es válido.
 *
 *
 * extiende Exception y usa @ResponseStatus para el mapeo HTTP automático.
 * Devuelve HTTP 400 Bad Request.
 *
 * Un cupón puede ser inválido por las siguientes razones:
 *   - El código no existe en la base de datos
 *   - El cupón está marcado como inactivo (activo = false)
 *   - La fecha de vencimiento ya pasó (fechaVencimiento < hoy)
 *   - El cupón es de uso único y ya fue utilizado previamente
 *
 * Lanzada en: CuponServiceImpl.validar() y OrderServiceImpl al aplicar el cupón.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "El cupón es inválido, está vencido o ya fue utilizado.")
public class CuponInvalidoException extends Exception {
}
