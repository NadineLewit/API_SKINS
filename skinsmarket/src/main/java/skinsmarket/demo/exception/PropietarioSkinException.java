package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un usuario intenta comprar una skin que él mismo publicó.
 *
 * Nueva excepción específica del marketplace de skins.
 * Sigue la misma estructura que las demás excepciones del proyecto.
 * Devuelve HTTP 400 Bad Request.
 *
 * Lanzada en: OrderServiceImpl.createOrder(), cuando se detecta que
 * la skin que se intenta comprar tiene como vendedor al mismo usuario
 * que está realizando la compra.
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST,
        reason = "No podés comprar una skin que vos mismo publicaste.")
public class PropietarioSkinException extends Exception {
}
