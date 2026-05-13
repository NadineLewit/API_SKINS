package skinsmarket.demo.service;

import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.Role;
import skinsmarket.demo.exception.EmailException;
import skinsmarket.demo.exception.PasswordException;
import skinsmarket.demo.repository.CarritoRepository;
import skinsmarket.demo.utils.InfoValidator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import skinsmarket.demo.controller.auth.AuthenticationRequest;
import skinsmarket.demo.controller.auth.AuthenticationResponse;
import skinsmarket.demo.controller.config.RegisterRequest;
import skinsmarket.demo.controller.config.RegistroResponse;
import skinsmarket.demo.controller.config.JwtService;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

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
    private final PasswordEncoder     passwordEncoder;
    private final JwtService          jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registra un nuevo usuario en el marketplace de skins.
     *
     * Devuelve AuthResponse con token + datos del usuario para que el
     * frontend no tenga que hacer un segundo request a /api/v1/users/me.
     * NO incluye el rol en la respuesta.
     *
     * @throws PasswordException si las contraseñas no cumplen los requisitos
     * @throws EmailException    si el email tiene formato inválido
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request)
            throws PasswordException, EmailException {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El body de registro es obligatorio");
        }

        validateRequiredRegisterFields(request);

        // 1. Validar contraseñas
        if (!InfoValidator.isValidPassword(request.getPassword(), request.getPasswordRepeat())) {
            throw new PasswordException();
        }

        String email = request.getEmail().trim();
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

        // 5. Generar token mínimo y devolver solo el access_token.
        // El frontend puede pedir el perfil a /api/v1/users/me si necesita más datos.
        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
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

        String email = request.getEmail().trim();

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
        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
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
}
