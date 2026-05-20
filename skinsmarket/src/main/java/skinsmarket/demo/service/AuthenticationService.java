package skinsmarket.demo.service;

import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.EmailVerificationToken;
import skinsmarket.demo.entity.PasswordResetToken;
import skinsmarket.demo.entity.Role;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.exception.PasswordException;
import skinsmarket.demo.repository.CarritoRepository;
import skinsmarket.demo.repository.EmailVerificationTokenRepository;
import skinsmarket.demo.repository.PasswordResetTokenRepository;
import skinsmarket.demo.utils.InfoValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import skinsmarket.demo.controller.auth.AuthenticationRequest;
import skinsmarket.demo.controller.auth.AuthenticationResponse;
import skinsmarket.demo.controller.auth.ForgotPasswordRequest;
import skinsmarket.demo.controller.auth.ResetPasswordRequest;
import skinsmarket.demo.controller.auth.VerifyEmailRequest;
import skinsmarket.demo.controller.config.RegisterRequest;
import skinsmarket.demo.controller.config.JwtService;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Servicio de autenticación: registro e inicio de sesión con JWT.
 *
 * Única diferencia: al registrar un usuario se crea un Carrito vacío
 * en lugar de una Wishlist (dominio de skins).
 *
 * Usa inyección por constructor con @RequiredArgsConstructor (Lombok).
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository      userRepository;
    private final CarritoRepository   carritoRepository;   // reemplaza WishlistRepository
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder     passwordEncoder;
    private final JwtService          jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${application.frontend.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    @Value("${application.frontend.verify-email-url:http://localhost:5173/verify-email}")
    private String verifyEmailUrl;

    /**
     * Registra un nuevo usuario en el marketplace de skins.
     *
     * Crea la cuenta sin habilitar login hasta que el usuario verifique
     * su email desde el link enviado al correo registrado.
     *
     * @throws PasswordException si las contraseñas no cumplen los requisitos
     * @throws EmailException    si el email tiene formato inválido
     */
    @Transactional
    public void register(RegisterRequest request)
            throws PasswordException, EmailException {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El body de registro es obligatorio");
        }

        validateRequiredRegisterFields(request);

        // 1. Validar contraseñas
        if (!InfoValidator.isValidPassword(request.getPassword(), request.getPasswordRepeat())) {
            throw new PasswordException();
        }

        String email = InfoValidator.normalizeEmail(request.getEmail());
        String username = request.getUsername().trim();

        // 2. Validar formato de email
        if (!InfoValidator.isValidEmail(email)) {
            throw new EmailException();
        }

        // 3. Verificar que el email no esté ya registrado
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        // 4. Verificar que el username no esté ya en uso
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de usuario ya está en uso");
        }

        // 3. Construir y guardar el usuario
        User user = User.builder()
                .username(username)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // 4. Crear un carrito vacío para el nuevo usuario si todavía no existe.
        // Esto evita chocar con datos viejos/orfandad de BD en el índice único user_id.
        carritoRepository.findByUser(user).orElseGet(() -> {
            Carrito carrito = new Carrito();
            carrito.setUser(user);
            carrito.setEstado(Carrito.Estado.VACIO);
            return carritoRepository.save(carrito);
        });

        sendEmailVerification(user);
    }

    /**
     * Autentica un usuario existente y devuelve token + datos básicos.
     *
     * La autenticación se realiza por EMAIL (no por username/nombre),
     * lo que simplifica el flujo del frontend.
     * La respuesta incluye email y firstName pero NO el rol.
     */
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email y contraseña son obligatorios");
        }

        String email = InfoValidator.normalizeEmail(request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales inválidas. Verificá tu email y contraseña.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tenés que verificar tu email antes de iniciar sesión.");
        }
        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        if (request == null || isBlank(request.getToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de verificación es obligatorio");
        }

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findByTokenHashAndUsedAtIsNull(hashToken(request.getToken().trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o vencido"));

        LocalDateTime now = LocalDateTime.now();
        if (verificationToken.isExpired(now)) {
            verificationToken.setUsedAt(now);
            emailVerificationTokenRepository.save(verificationToken);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o vencido");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        verificationToken.setUsedAt(now);

        userRepository.save(user);
        emailVerificationTokenRepository.save(verificationToken);
    }

    @Transactional
    public void resendVerification(ForgotPasswordRequest request) {
        if (request == null || isBlank(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo email es obligatorio");
        }

        String email = InfoValidator.normalizeEmail(request.getEmail());
        if (!InfoValidator.isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo email debe tener un formato válido");
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                sendEmailVerification(user);
            }
        });
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        if (request == null || isBlank(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo email es obligatorio");
        }

        String email = InfoValidator.normalizeEmail(request.getEmail());
        if (!InfoValidator.isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo email debe tener un formato válido");
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            sendPasswordReset(user);
        });
    }

    @Transactional
    public void requestPasswordResetForAuthenticatedUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        sendPasswordReset(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) throws PasswordException {
        if (request == null || isBlank(request.getToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token de recuperación es obligatorio");
        }

        if (!InfoValidator.isValidPassword(request.getPassword(), request.getPasswordRepeat())) {
            throw new PasswordException();
        }

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNull(hashToken(request.getToken().trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o vencido"));

        LocalDateTime now = LocalDateTime.now();
        if (resetToken.isExpired(now)) {
            resetToken.setUsedAt(now);
            passwordResetTokenRepository.save(resetToken);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o vencido");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(true);
        resetToken.setUsedAt(now);

        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
    }

    private void validateRequiredRegisterFields(RegisterRequest request) {
        if (isBlank(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo username es obligatorio");
        }
        if (isBlank(request.getFirstName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo firstName es obligatorio");
        }
        if (isBlank(request.getLastName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo lastName es obligatorio");
        }
        if (isBlank(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo email es obligatorio");
        }
        if (isBlank(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo password es obligatorio");
        }
        if (isBlank(request.getPasswordRepeat())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo passwordRepeat es obligatorio");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildResetLink(String rawToken) {
        return resetPasswordUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private String buildVerifyEmailLink(String rawToken) {
        return verifyEmailUrl + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private void sendEmailVerification(User user) {
        emailVerificationTokenRepository.deleteByUserAndUsedAtIsNull(user);

        String rawToken = generateToken();
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .tokenHash(hashToken(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusHours(24))
                .build();

        emailVerificationTokenRepository.save(verificationToken);
        emailService.sendEmailVerificationEmail(user.getEmail(), buildVerifyEmailLink(rawToken));
    }

    private void sendPasswordReset(User user) {
        passwordResetTokenRepository.deleteByUserAndUsedAtIsNull(user);

        String rawToken = generateToken();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .tokenHash(hashToken(rawToken))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusMinutes(30))
                .build();

        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), buildResetLink(rawToken));
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hashear el token de recuperación", e);
        }
    }
}
