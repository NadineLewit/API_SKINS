package skinsmarket.demo.controller.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.exception.PasswordException;

/**
 * Manejo centralizado de errores no atrapados por controllers específicos.
 * Evita exponer stack traces en las respuestas HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException e) {
        String message = e.getReason() != null ? e.getReason() : e.getStatusCode().toString();
        return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.of(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of("La operación viola una restricción de datos existente"));
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmail(EmailException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of("El campo email debe tener un formato válido y no estar registrado por otro usuario."));
    }

    @ExceptionHandler(PasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handlePassword(PasswordException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.of("Las contraseñas deben coincidir, tener al menos 5 caracteres, una letra y un número."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException e) {
        return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ApiResponse.of("Error interno del servidor"));
    }
}
