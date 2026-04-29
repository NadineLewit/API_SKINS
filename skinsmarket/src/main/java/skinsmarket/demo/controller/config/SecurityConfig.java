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
 *   - Se agregó el catálogo maestro de skins:
 *     · GET  /catalogo/**           → público (cualquiera puede ver el catálogo)
 *     · POST /catalogo              → ADMIN (crear manualmente)
 *     · POST /catalogo/sincronizar  → ADMIN (importar desde la API de ByMykel)
 *     · DELETE /catalogo/{id}       → ADMIN
 *
 * Política de sesiones: STATELESS (sin estado de sesión en servidor, todo via JWT).
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
                .requestMatchers("/categories").permitAll()
                .requestMatchers("/skins/get/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // ── Catálogo: GET público, escritura solo ADMIN ─────────────────
                // Todos pueden consultar el catálogo (es la "base de datos de skins")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/catalogo").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/catalogo/**").permitAll()
                // Solo ADMIN puede modificar el catálogo (crear, sincronizar, eliminar)
                .requestMatchers(org.springframework.http.HttpMethod.POST,   "/catalogo/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/catalogo/**").hasAnyAuthority(Role.ADMIN.name())

                // ── Rutas exclusivas de ADMIN ───────────────────────────────────
                .requestMatchers("/categories/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers("/skins/admin/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers("/cupones/validar").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers("/cupones/**").hasAnyAuthority(Role.ADMIN.name())
                .requestMatchers("/api/v1/admin/**").hasAnyAuthority(Role.ADMIN.name())

                // ── Rutas de VENDEDOR (USER autenticado puede publicar y gestionar sus skins) ──
                // POST /skins/with-image     → publicar nueva skin (catalogoId obligatorio para USER)
                // PUT  /skins/{id}/with-image → editar su propia skin
                // PUT  /skins/{id}/inactivar  → inactivar su propia skin (baja lógica)
                .requestMatchers(org.springframework.http.HttpMethod.POST,   "/skins/with-image").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/skins/*/with-image").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/skins/*/inactivar").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
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
