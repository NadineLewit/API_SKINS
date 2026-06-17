package skinsmarket.demo.controller.user;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.service.AuthenticationService;
import skinsmarket.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión del perfil del usuario autenticado.
 *
 * Permite a cada usuario ver y actualizar su propia información de perfil.
 *
 * Todas las rutas requieren autenticación.
 * Ruta base: /api/v1/users
 *
 * Todas las respuestas siguen el formato uniforme ApiResponse.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final AuthenticationService authenticationService;

    /**
     * Actualiza los datos del perfil del usuario autenticado.
     * PUT /api/v1/users/me
     *
     * La contraseña no se cambia desde este endpoint: se solicita por mail con
     * POST /api/v1/users/me/password-reset-email y se confirma con el token.
     */
    @PutMapping("/me")
    public ResponseEntity<?> actualizarUser(
            Authentication auth,
            @RequestBody UserRequest request) throws EmailException {

        UserResponse actualizado = userService.actualizarUser(auth.getName(), request);

        return ResponseEntity.ok(
                ApiResponse.of("Perfil actualizado exitosamente", actualizado));
    }

    /**
     * Devuelve los datos del perfil del usuario autenticado.
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getUserByEmail(Authentication auth) {
        UserResponse user = userService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(ApiResponse.of("Perfil del usuario autenticado", user));
    }

    /**
     * Envía al mail registrado un link para cambiar la contraseña.
     * POST /api/v1/users/me/password-reset-email
     */
    @PostMapping("/me/password-reset-email")
    public ResponseEntity<ApiResponse<Void>> requestPasswordResetEmail(Authentication auth) {
        authenticationService.requestPasswordResetForAuthenticatedUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.of(
                "Te enviamos un mail con el link para cambiar la contraseña."));
    }
}
