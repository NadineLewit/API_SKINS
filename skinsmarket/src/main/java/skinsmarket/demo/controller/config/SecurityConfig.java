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
 * CAMBIOS RECIENTES:
 *   - Las skins ya NO se eliminan con DELETE: ahora se inactivan con
 *     PUT /skins/{id}/inactivar y PUT /skins/admin/inactivar/{id}.
 *     Razón: como es baja lógica (cambio de atributo, no borrado físico),
 *     DELETE era semánticamente incorrecto.
 *
 * Política de sesiones: STATELESS (sin estado de sesión en servidor, todo via JWT).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * Define las reglas de autorización para cada ruta de la API.
     *
     * PÚBLICAS (sin token):
     *   - /api/v1/auth/**              → registro y login
     *   - /categories                  → listar categorías (catálogo)
     *   - /skins/get/**                → ver catálogo de skins
     *   - /uploads/**                  → acceder a imágenes subidas
     *   - /error/**                    → páginas de error de Spring
     *
     * SOLO ADMIN:
     *   - /categories/**               → crear, editar, eliminar categorías
     *   - /skins/admin/**              → ABM de skins desde el panel de admin
     *                                    (incluye PUT /skins/admin/inactivar/{id})
     *   - /cupones/**                  → gestión y listado de cupones
     *   - /api/v1/admin/**             → panel de administración general
     *
     * VENDEDOR (USER autenticado o ADMIN):
     *   - POST /skins/with-image       → publicar nueva skin
     *   - PUT  /skins/{id}/with-image  → editar su propia skin
     *   - PUT  /skins/{id}/inactivar   → inactivar su propia skin (baja lógica)
     *   - GET  /skins/mis-skins        → listar sus propias skins
     *
     * USER y ADMIN:
     *   - /carrito/**                  → gestión del carrito de compras
     *   - /order/**                    → crear y ver órdenes propias
     *   - /cupones/validar             → validar un cupón antes de comprar
     *
     * CUALQUIER AUTENTICADO:
     *   - /api/v1/users/**             → ver y editar el propio perfil
     */
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
                .requestMatchers("/categories").permitAll()
                .requestMatchers("/skins/get/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // ── Rutas exclusivas de ADMIN ───────────────────────────────────
                .requestMatchers("/categories/**").hasAnyAuthority(Role.ADMIN.name())
                // /skins/admin/** cubre también PUT /skins/admin/inactivar/{id}
                .requestMatchers("/skins/admin/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers("/cupones/validar").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/cupones/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority(Role.ADMIN.name())

                // ── Rutas de VENDEDOR (USER autenticado puede publicar y gestionar sus skins) ──
                // POST /skins/with-image     → publicar nueva skin
                // PUT  /skins/{id}/with-image → editar su propia skin
                // PUT  /skins/{id}/inactivar  → inactivar su propia skin (baja lógica)
                .requestMatchers(org.springframework.http.HttpMethod.POST,   "/skins/with-image").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/skins/*/with-image").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/skins/*/inactivar").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                // GET /skins/mis-skins: cualquier usuario autenticado
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/skins/mis-skins").authenticated()

                // ── Carrito y órdenes: USER y ADMIN ─────────────────────────────
                .requestMatchers("/carrito/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/order/**").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())

                // ── Perfil de usuario: cualquier autenticado ────────────────────
                .requestMatchers("/api/v1/users/**").authenticated()

                // Cualquier otra ruta requiere autenticación válida
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuración de CORS (Cross-Origin Resource Sharing).
     */
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
