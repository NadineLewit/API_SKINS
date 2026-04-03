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
 * Estructura idéntica al SecurityConfig del TPO aprobado.
 * Solo se modificaron las rutas para que coincidan con los nuevos controllers:
 *   - /games/**  → /skins/**
 *   - /wishlist/** → /carrito/**
 *   - Se agregaron rutas para /cupones/**
 *
 * Política de sesiones: STATELESS (sin estado de sesión en servidor, todo via JWT).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Filtro JWT que intercepta cada request para validar el token
    private final JwtAuthenticationFilter jwtAuthFilter;

    // Proveedor de autenticación configurado en ApplicationConfig
    private final AuthenticationProvider authenticationProvider;

    /**
     * Define las reglas de autorización para cada ruta de la API.
     *
     * Lógica de acceso por ruta:
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
     *   - /cupones/**                  → gestión y listado de cupones
     *   - /api/v1/admin/**             → panel de administración general
     *
     * SOLO USER (usuario autenticado):
     *   - /carrito/**                  → gestión del carrito de compras
     *   - /order/**                    → crear y ver órdenes propias
     *
     * CUALQUIER AUTENTICADO:
     *   - /cupones/validar             → validar un cupón antes de comprar
     *   - /api/v1/users/**             → ver y editar el propio perfil
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Deshabilitamos CSRF ya que usamos JWT (stateless, no hay sesión de browser)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req

                        // ── Rutas completamente públicas ────────────────────────────────
                        // Autenticación: registro y login
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Páginas de error generadas por Spring
                        .requestMatchers("/error/**").permitAll()
                        // Catálogo público: listar categorías sin autenticarse
                        .requestMatchers("/categories").permitAll()
                        // Catálogo público: ver skins disponibles y filtros
                        .requestMatchers("/skins/get/**").permitAll()
                        // Archivos de imagen subidos al servidor
                        .requestMatchers("/uploads/**").permitAll()

                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()

                        // ── Rutas exclusivas de ADMIN ───────────────────────────────────
                        // Gestión de categorías (crear, editar, eliminar)
                        .requestMatchers("/categories/**").hasAnyAuthority(Role.ADMIN.name())
                        // ABM de skins desde el panel de administración (todas las skins)
                        .requestMatchers("/skins/admin/**").hasAnyAuthority(Role.ADMIN.name())
                        // Validar cupón: el USER lo necesita para ver el descuento antes de confirmar compra
                        .requestMatchers("/cupones/validar").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                        // Resto de gestión de cupones (crear, listar, eliminar): solo ADMIN
                        .requestMatchers("/cupones/**").hasAnyAuthority(Role.ADMIN.name())
                        // Panel de administración: usuarios, órdenes, roles
                        .requestMatchers("/api/v1/admin/**").hasAnyAuthority(Role.ADMIN.name())

                        // ── Rutas de VENDEDOR (USER autenticado puede publicar y gestionar sus skins) ──
                        // Requisito TPO: "usuarios registrados como vendedores podrán publicar productos"
                        // POST /skins     → publicar nueva skin
                        // PUT /skins/{id} → editar su propia skin
                        // DELETE /skins/{id} → eliminar su propia skin
                        .requestMatchers(org.springframework.http.HttpMethod.POST,   "/skins").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/skins/*").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/skins/*").hasAnyAuthority(Role.USER.name(), Role.ADMIN.name())

                        // ── Rutas exclusivas de USER autenticado ────────────────────────
                        // Gestión del carrito de compras
                        .requestMatchers("/carrito/**").hasAnyAuthority(Role.USER.name())
                        // Crear y consultar órdenes propias
                        .requestMatchers("/order/**").hasAnyAuthority(Role.USER.name())

                        // ── Perfil de usuario: cualquier autenticado ────────────────────
                        // IMPORTANTE: debe ser authenticated() y NO permitAll().
                        // Si fuera permitAll(), cualquier request sin token llegaría al controller
                        // y auth.getName() lanzaría NullPointerException.
                        .requestMatchers("/api/v1/users/**").authenticated()

                        // Cualquier otra ruta requiere autenticación válida
                        .anyRequest().authenticated()
                )
                // Sin estado de sesión: cada request se autentica via JWT
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                // El filtro JWT se ejecuta antes del filtro estándar de usuario/contraseña
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}