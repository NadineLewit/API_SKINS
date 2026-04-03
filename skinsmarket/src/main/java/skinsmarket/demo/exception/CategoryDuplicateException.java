package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada al intentar crear una categoría cuyo nombre ya existe.
 *
 * @ResponseStatus mapea automáticamente esta excepción a una respuesta HTTP 400
 * con el mensaje indicado, sin necesidad de manejarla manualmente en el controller.
 *
 * Lanzada en: CategoryServiceImpl.createCategory()
 */
@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "La categoria que se intenta agregar ya existe")
public class CategoryDuplicateException extends Exception {
}
