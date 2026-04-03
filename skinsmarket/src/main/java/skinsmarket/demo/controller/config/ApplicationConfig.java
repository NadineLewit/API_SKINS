package skinsmarket.demo.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import skinsmarket.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Configuración de la infraestructura de autenticación de Spring Security.
 *
 * Define los beans necesarios para que Spring Security pueda:
 *   - Cargar usuarios desde la base de datos (UserDetailsService)
 *   - Verificar contraseñas hasheadas (PasswordEncoder)
 *   - Autenticar usuarios (AuthenticationProvider y AuthenticationManager)
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    // Repositorio de usuarios necesario para cargar los datos del usuario autenticado
    private final UserRepository repository;

    /**
     * Define cómo Spring Security carga un usuario dado su username (email).
     *
     * Se busca el usuario por email en la base de datos.
     * Si no existe, lanza UsernameNotFoundException para que Spring devuelva 403.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /**
     * Configura el proveedor de autenticación basado en base de datos (DAO).
     *
     * Combina el UserDetailsService (cómo cargar usuarios) con el PasswordEncoder
     * (cómo verificar contraseñas). Spring Security usa esto para autenticar.
     */
    @SuppressWarnings("deprecation")
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    /**
     * Expone el AuthenticationManager como bean para poder usarlo en el AuthenticationService.
     *
     * Es necesario para realizar la autenticación manual al hacer login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define el algoritmo de hashing de contraseñas: BCrypt.
     *
     * BCrypt es el estándar de la industria para almacenar contraseñas de forma segura.
     * Genera un salt aleatorio en cada hash, lo que lo hace resistente a ataques
     * de tabla arco iris (rainbow table attacks).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
