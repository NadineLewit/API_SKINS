package skinsmarket.demo.controller.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import skinsmarket.demo.entity.User;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Servicio para la generación y validación de tokens JWT.
 *
 *
 * Responsabilidades:
 *   - Generar tokens JWT firmados con la clave secreta
 *   - Extraer claims (datos) de un token (username, rol, userId, etc.)
 *   - Validar que un token sea auténtico y no haya expirado
 *
 * La clave secreta y el tiempo de expiración se leen desde application.properties.
 */
@Service
public class JwtService {

    // Clave secreta para firmar los tokens (debe tener mínimo 256 bits para HS256)
    // Se configura en application.properties: application.security.jwt.secretKey
    @Value("${application.security.jwt.secretKey}")
    private String secretKey;

    // Tiempo de expiración del token en milisegundos
    // Se configura en application.properties: application.security.jwt.expiration
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Genera un token JWT para el usuario autenticado.
     *
     * El token incluye como claims:
     *   - subject: email del usuario
     *   - role: rol del usuario (USER o ADMIN)
     *   - userId: ID del usuario en la base de datos
     *   - issuedAt: fecha de emisión
     *   - expiration: fecha de expiración
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtExpiration);
    }

    /**
     * Construye el token JWT con todos sus claims.
     *
     * @param userDetails datos del usuario (implementa la interfaz de Spring Security)
     * @param expiration  tiempo en milisegundos hasta que expire el token
     */
    private String buildToken(UserDetails userDetails, long expiration) {

        // Casteamos a nuestra entidad User para acceder al ID
        User user = (User) userDetails;
        Long userId = user.getId();

        // Extraemos el primer rol del usuario (asumimos un rol por usuario)
        String userRole = userDetails.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("USER"); // Rol por defecto si no se encuentra ninguno

        return Jwts
                .builder()
                // El subject es el email (identificador principal del usuario)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                // Claims personalizados: rol e ID del usuario
                .claim("role", userRole)
                .claim("userId", userId)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // Firmamos el token con la clave secreta HMAC-SHA256
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * Valida que el token pertenezca al usuario y no haya expirado.
     *
     * @param token       token JWT a validar
     * @param userDetails datos del usuario contra el que se valida
     * @return true si el token es válido, false en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractClaim(token, Claims::getSubject);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Verifica si el token ha expirado comparando la fecha de expiración con la actual.
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Extrae el email (username/subject) del token JWT.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae un claim específico del token usando una función de resolución.
     *
     * @param token          token JWT del que se extrae el claim
     * @param claimsResolver función que define qué claim extraer
     * @param <T>            tipo del claim a extraer
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parsea y devuelve todos los claims del token JWT.
     *
     * Lanzará una excepción si el token está mal formado, fue alterado
     * o la firma no coincide con la clave secreta.
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Construye la clave secreta HMAC a partir del string configurado en properties.
     *
     * Se usa HMAC-SHA256 (HS256), el algoritmo de firma simétrico más común para JWT.
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
