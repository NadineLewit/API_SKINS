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
import org.springframework.stereotype.Service;

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
     * Devuelve RegistroResponse con token + datos del usuario para que el
     * frontend no tenga que hacer un segundo request a /api/v1/users/me.
     * NO incluye el rol en la respuesta.
     *
     * @throws PasswordException si las contraseñas no cumplen los requisitos
     * @throws EmailException    si el email tiene formato inválido
     */
    public RegistroResponse register(RegisterRequest request)
            throws PasswordException, EmailException {

        // 1. Validar contraseñas
        if (!InfoValidator.isValidPassword(request.getPassword(), request.getPasswordRepeat())) {
            throw new PasswordException();
        }

        // 2. Validar formato de email
        if (!InfoValidator.isValidEmail(request.getEmail())) {
            throw new EmailException();
        }

        // 3. Verificar que el email no esté ya registrado
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailException();
        }

        // 4. Verificar que el username no esté ya en uso
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }

        // 3. Construir y guardar el usuario
        User user = User.builder()
                .username(request.getUsername())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // 4. Crear un carrito vacío para el nuevo usuario
        Carrito carrito = new Carrito();
        carrito.setUser(user);
        carrito.setEstado(Carrito.Estado.VACIO);
        carritoRepository.save(carrito);

        // 5. Generar token y devolver RegistroResponse con datos del usuario
        String jwtToken = jwtService.generateToken(user);
        return RegistroResponse.builder()
                .accessToken(jwtToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
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
        // Spring Security verifica email y contraseña contra la BD
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        // Credenciales correctas: generar token y devolver datos básicos
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .build();
    }
}