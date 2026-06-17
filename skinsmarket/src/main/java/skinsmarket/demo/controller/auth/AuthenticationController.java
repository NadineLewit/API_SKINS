package skinsmarket.demo.controller.auth;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.exception.PasswordException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skinsmarket.demo.service.AuthenticationService;

import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la autenticación de usuarios.
 *
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
    public ResponseEntity<ApiResponse<Void>> register(
            @RequestBody skinsmarket.demo.controller.config.RegisterRequest request) throws PasswordException, EmailException {
        service.register(request);
        return ResponseEntity.ok(ApiResponse.of(
                "Registro exitoso. Te enviamos un mail para verificar tu cuenta."));
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

    /**
     * Verifica el email de una cuenta recién registrada.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestBody VerifyEmailRequest request) {
        service.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.of("Email verificado exitosamente"));
    }

    /**
     * Reenvía el mail de verificación sin revelar si el email existe.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestBody ForgotPasswordRequest request) {
        service.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.of(
                "Si el email está registrado y pendiente de verificación, te reenviamos el link."));
    }

    /**
     * Solicita un link de recuperación para el email registrado.
     * La respuesta es genérica para no revelar si el email existe o no.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        service.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.of(
                "Si el email está registrado, te enviamos un link para cambiar la contraseña."));
    }

    /**
     * Cambia la contraseña usando el token recibido por email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestBody ResetPasswordRequest request) throws PasswordException {
        service.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.of("Contraseña actualizada exitosamente"));
    }
}
