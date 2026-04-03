package skinsmarket.demo.controller.config;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Filtro de autenticación JWT que se ejecuta una sola vez por request.
 *
 *
 * Funcionamiento (ejecutado en cada request HTTP):
 * 1. Extrae el token JWT del header "Authorization: Bearer <token>"
 * 2. Valida el token usando JwtService
 * 3. Si es válido, carga el usuario y lo registra en el SecurityContext
 * 4. Pasa el request al siguiente filtro de la cadena
 *
 * Extiende OncePerRequestFilter para garantizar que se ejecuta exactamente
 * una vez por request, evitando procesamiento duplicado.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Servicio para operar con los tokens JWT (extraer claims, validar, etc.)
    private final JwtService jwtService;

    // Servicio para cargar los detalles del usuario desde la base de datos
    private final UserDetailsService userDetailsService;

    /**
     * Lógica principal del filtro: valida el token JWT y autentica al usuario.
     *
     * Si el header Authorization no está presente o no tiene formato "Bearer ...",
     * el filtro deja pasar el request sin autenticar (Spring Security lo rechazará
     * si la ruta requiere autenticación).
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraer el header de autorización
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Si no hay header o no empieza con "Bearer", dejamos pasar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer el token (quitar los primeros 7 caracteres: "Bearer ")
        jwt = authHeader.substring(7);

        // 3. Extraer el email (subject) del token
        userEmail = jwtService.extractUsername(jwt);

        // 4. Si tenemos un email y el usuario aún no está autenticado en el contexto
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Cargar el usuario desde la base de datos
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Validar que el token sea válido (no expirado y corresponde al usuario)
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Crear el token de autenticación de Spring con los roles del usuario
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // Sin credenciales (ya está autenticado)
                                userDetails.getAuthorities());

                // Agregar detalles del request (IP, session, etc.)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // Registrar la autenticación en el contexto de seguridad de Spring
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 5. Continuar con el siguiente filtro en la cadena
        filterChain.doFilter(request, response);
    }
}
