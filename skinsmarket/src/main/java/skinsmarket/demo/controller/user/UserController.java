package skinsmarket.demo.controller.user;

import skinsmarket.demo.controller.auth.AuthenticationRequest;
import skinsmarket.demo.controller.auth.AuthenticationResponse;
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
 * Idéntico al UserController del TPO aprobado.
 * Permite a cada usuario ver y actualizar su propia información de perfil.
 *
 * Todas las rutas requieren autenticación.
 * Ruta base: /api/v1/users
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    // Inyección del servicio de usuarios via constructor (práctica recomendada con Lombok)
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
     * Si solo actualiza otros datos (nombre, apellido, email), devuelve 204 No Content.
     *
     * @throws EmailException si el nuevo email ya está en uso por otro usuario
     */
    @PutMapping("/me")
    public ResponseEntity<?> actualizarUser(
            Authentication auth,
            @RequestBody UserRequest request) throws EmailException {

        // Actualizamos los datos del usuario autenticado (identificado por su email en el token)
        UserResponse actualizado = userService.actualizarUser(auth.getName(), request);

        // Si se actualizó la contraseña, generamos un nuevo token JWT y lo devolvemos
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            AuthenticationRequest authReq = new AuthenticationRequest(
                    actualizado.getEmail(), request.getPassword());
            AuthenticationResponse authResp = authenticationService.authenticate(authReq);
            return ResponseEntity.ok(authResp);
        }

        // Sin cambio de contraseña: confirmamos con 204 No Content
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve los datos del perfil del usuario autenticado.
     * GET /api/v1/users/me
     *
     * Utiliza el email del token JWT para identificar al usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserByEmail(Authentication auth) {
        // Obtenemos el email del token JWT y consultamos el servicio
        UserResponse user = userService.getUserByEmail(auth.getName());
        return ResponseEntity.ok(user);
    }
}