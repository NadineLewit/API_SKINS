package skinsmarket.demo.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

import java.io.IOException;
import java.time.Duration;

/**
 * Configuración de la infraestructura de autenticación de Spring Security
 * y otros beans transversales del proyecto.
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository repository;

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    @SuppressWarnings("deprecation")
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider =
                new DaoAuthenticationProvider(userDetailsService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * RestTemplate para consumir APIs externas (Steam, ByMykel/CSGO-API).
     *
     * IMPORTANTE — Decisión sobre el User-Agent:
     * Steam tiene un anti-scraper: cuando recibe un request con User-Agent
     * de browser (Mozilla, Firefox, Chrome) PERO sin las cookies de sesión
     * que un browser real tendría, lo detecta como scraper y devuelve 400
     * Bad Request.
     *
     * En cambio, User-Agents simples de clientes HTTP (curl, wget, Java)
     * pasan sin problema, porque Steam asume que son herramientas legítimas.
     *
     * Por eso usamos "curl/8.18.0" — el mismo User-Agent que devuelve HTTP 200
     * cuando hacés:
     *   curl "https://steamcommunity.com/inventory/{steamId}/730/2?l=english"
     *
     * Probado y verificado: con User-Agent de Firefox → 400, con curl → 200.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new SimpleUserAgentInterceptor());
        return restTemplate;
    }

    /**
     * Interceptor que mantiene los headers MÍNIMOS, igual que un curl puro.
     * Cualquier header extra que parezca de browser dispara el anti-scraper
     * de Steam. Mantener simple es fundamental.
     */
    private static class SimpleUserAgentInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution)
                throws IOException {
            request.getHeaders().set("User-Agent", "curl/8.18.0");
            request.getHeaders().set("Accept", "*/*");
            return execution.execute(request, body);
        }
    }
}
