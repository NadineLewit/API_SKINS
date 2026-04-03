package skinsmarket.demo.controller.auth;

import skinsmarket.demo.controller.config.RegistroResponse;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.exception.PasswordException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinsmarket.demo.controller.auth.AuthenticationRequest;
import skinsmarket.demo.controller.auth.AuthenticationResponse;

import skinsmarket.demo.controller.config.RegisterRequest;
import skinsmarket.demo.service.AuthenticationService;

import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la autenticación de usuarios.
 *
 * Idéntico al AuthenticationController del TPO aprobado.
 * Maneja el registro de nuevos usuarios y el inicio de sesión,
 * devolviendo un token JWT en ambos casos.
 *
 * Rutas públicas (sin autenticación requerida):
 *   POST /api/v1/auth/register      — registrar cuenta nueva
 *   POST /api/v1/auth/authenticate  — iniciar sesión
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    // Inyección del servicio de autenticación via constructor (práctica recomendada con Lombok)
    private final AuthenticationService service;

    /**
     * Registra un nuevo usuario en el marketplace de skins.
     * POST /api/v1/auth/register
     *
     * Devuelve RegistroResponse con el token JWT y los datos básicos del usuario
     * para que el frontend pueda mostrar la bienvenida sin un request extra.
     *
     * @throws PasswordException si las contraseñas no coinciden o no cumplen los requisitos
     * @throws EmailException    si el email ya está registrado o tiene formato inválido
     */
    @PostMapping("/register")
    public ResponseEntity<RegistroResponse> register(
            @RequestBody skinsmarket.demo.controller.config.RegisterRequest request) throws PasswordException, EmailException {
        return ResponseEntity.ok(service.register(request));
    }

    /**
     * Autentica a un usuario existente y devuelve un token JWT.
     * POST /api/v1/auth/authenticate
     *
     * El token devuelto debe incluirse en el header Authorization: Bearer <token>
     * en todas las peticiones que requieran autenticación.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }
}