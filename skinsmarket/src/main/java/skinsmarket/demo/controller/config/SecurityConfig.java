package skinsmarket.demo.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import skinsmarket.demo.entity.Role;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración de seguridad de Spring Security para el marketplace de skins.
 *
 * Los USER ahora publican exclusivamente vía /inventario/{id}/publicar,
 * que requiere que el item esté en su inventario REAL de Steam.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(req -> req

                // ── Rutas completamente públicas ────────────────────────────────
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/error/**").permitAll()
                .requestMatchers("/skins/get/**").permitAll()
                .requestMatchers("/historial/**").permitAll()
                .requestMatchers("/ranking/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/payments/return/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/payments/webhook").permitAll()

                // ── Catálogo: GET público, escritura solo ADMIN ─────────────────
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/catalogo").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/catalogo/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST,   "/catalogo/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/catalogo/**").hasAnyAuthority(Role.ADMIN.name())

                // ── Inventario de Steam: USER y ADMIN autenticados ──────────────
                .requestMatchers("/inventario/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())

                // ── Rutas exclusivas de ADMIN ───────────────────────────────────
                .requestMatchers("/skins/admin/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers("/cupones/validar").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/cupones/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/operations/*/user-trade-received")
                    .hasAnyAuthority(Role.ADMIN.name())

                // ── Skins (vendedor) ────────────────────────────────────────────
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/skins/*/inactivar").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/skins/*").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/skins/mis-skins").authenticated()

                // ── Carrito y órdenes ───────────────────────────────────────────
                .requestMatchers("/carrito/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/order/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/operations/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())

                // ── Perfil ──────────────────────────────────────────────────────
                .requestMatchers("/api/v1/users/**").authenticated()

                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
