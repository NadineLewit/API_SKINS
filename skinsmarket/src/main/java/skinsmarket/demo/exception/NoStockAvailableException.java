package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando no hay suficiente stock disponible para completar
 * la cantidad solicitada en una orden de compra.
 *
 * Devuelve HTTP 400 Bad Request con el mensaje correspondiente.
 *
 * Lanzada en: OrderServiceImpl al validar el stock de cada skin
 * antes de confirmar la compra. Si alguna skin no tiene el stock
 * suficiente, se rechaza toda la orden.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "No hay suficiente stock disponible.")
public class NoStockAvailableException extends Exception {
}
