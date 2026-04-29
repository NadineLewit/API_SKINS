package skinsmarket.demo.controller.user;

import skinsmarket.demo.controller.auth.AuthenticationRequest;
import skinsmarket.demo.controller.auth.AuthenticationResponse;
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

    // Necesario para regenerar el token JWT si el usuario cambia su contraseña
    private final AuthenticationService authenticationService;

    /**
     * Actualiza los datos del perfil del usuario autenticado.
     * PUT /api/v1/users/me
     *
     * Si el usuario cambia su contraseña, se genera y devuelve un nuevo token JWT
     * para que pueda seguir usando la aplicación sin necesidad de volver a loguearse.
     *
     * Si solo actualiza otros datos (nombre, apellido, email, username), devuelve
     * un mensaje de confirmación con los datos actualizados.
     */
    @PutMapping("/me")
    public ResponseEntity<?> actualizarUser(
            Authentication auth,
            @RequestBody UserRequest request) throws EmailException {

        UserResponse actualizado = userService.actualizarUser(auth.getName(), request);

        // Si se actualizó la contraseña, generamos un nuevo token JWT y lo devolvemos
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            AuthenticationRequest authReq = new AuthenticationRequest(
                    actualizado.getEmail(), request.getPassword());
            AuthenticationResponse authResp = authenticationService.authenticate(authReq);
            return ResponseEntity.ok(
                    ApiResponse.of("Perfil y contraseña actualizados — nuevo token generado", authResp));
        }

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
}
