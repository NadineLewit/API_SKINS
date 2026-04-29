package skinsmarket.demo.controller.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO genérico para respuestas consistentes de la API.
 *
 * Estructura: { "message": "...", "data": { ... } }
 * El campo "data" se omite del JSON si es null.
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String message;
    private T data;

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    public static ApiResponse<Void> of(String message) {
        return new ApiResponse<>(message, null);
    }
}
