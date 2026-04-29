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
import org.springframework.web.client.RestTemplate;

import skinsmarket.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Configuración de la infraestructura de autenticación de Spring Security
 * y otros beans transversales del proyecto.
 *
 * Define los beans necesarios para que Spring Security pueda:
 *   - Cargar usuarios desde la base de datos (UserDetailsService)
 *   - Verificar contraseñas hasheadas (PasswordEncoder)
 *   - Autenticar usuarios (AuthenticationProvider y AuthenticationManager)
 *
 * Y un bean de RestTemplate que se usa para consumir APIs externas
 * (por ejemplo, la API de ByMykel/CSGO-API para sincronizar el catálogo).
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository repository;

    /**
     * Carga un usuario por su email para autenticarlo.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    /**
     * Proveedor de autenticación basado en BD (DAO) + BCrypt.
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
     * AuthenticationManager para realizar login manual desde el AuthenticationService.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt para hashear contraseñas.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * RestTemplate para consumir APIs externas (en este proyecto, la API pública
     * de skins de ByMykel/CSGO-API para sincronizar el catálogo).
     *
     * Es un bean único reutilizable — se inyecta en SkinCatalogoServiceImpl.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
