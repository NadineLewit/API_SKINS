package skinsmarket.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se intenta acceder a una skin que no existe
 * o que fue dada de baja (active = false).
 *
 * Nueva excepción respecto al TPO aprobado, específica del dominio de skins.
 * Sigue la misma estructura que las excepciones del TPO aprobado:
 * extiende Exception y usa @ResponseStatus para el mapeo HTTP automático.
 * Devuelve HTTP 404 Not Found (a diferencia de las otras excepciones de negocio
 * que devuelven 400, esta indica que el recurso simplemente no existe).
 *
 * Casos de uso:
 *   - Buscar una skin por ID y no existe (findById devuelve empty)
 *   - Intentar agregar al carrito una skin con active = false
 *   - Intentar comprar una skin que fue eliminada por su vendedor
 *
 * Lanzada en: SkinServiceImpl, CarritoServiceImpl, OrderServiceImpl.
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND,
        reason = "La skin no está disponible o no existe.")
public class SkinNoDisponibleException extends Exception {
}
