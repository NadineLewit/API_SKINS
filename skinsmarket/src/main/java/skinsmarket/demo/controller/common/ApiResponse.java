package skinsmarket.demo.controller.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO genérico para respuestas consistentes de la API.
 *
 * Estructura: { "message": "...", "data": { ... } }
 *
 * El campo "data" se omite del JSON si es null (gracias a @JsonInclude(NON_NULL)),
 * por lo que respuestas sin payload se ven simplemente como { "message": "..." }.
 *
 * Uso:
 *   ApiResponse.of("Skin creada exitosamente", skin)   → con datos
 *   ApiResponse.of("Categoría eliminada")              → solo mensaje
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Mensaje descriptivo para el cliente. */
    private String message;

    /** Datos opcionales devueltos en la respuesta (entidad, lista, etc.). */
    private T data;

    /**
     * Construye una respuesta con mensaje y datos.
     */
    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    /**
     * Construye una respuesta con solo mensaje (sin datos).
     */
    public static ApiResponse<Void> of(String message) {
        return new ApiResponse<>(message, null);
    }
}
