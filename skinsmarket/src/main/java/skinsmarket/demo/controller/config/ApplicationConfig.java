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
     * RestTemplate para consumir APIs externas (Steam, ByMykel/CSGO-API, etc.).
     *
     * Mejoras críticas:
     *   1. TIMEOUT EXPLÍCITO: 30s connect + 60s read.
     *   2. USER-AGENT DE NAVEGADOR: Steam discrimina por UA.
     *   3. ACCEPT-ENCODING: identity → IMPORTANTE — le decimos a Steam que NO
     *      comprima la respuesta. Sin esto, Steam manda gzip cuando ve que el
     *      User-Agent parece un browser, y el RestTemplate (sin SDK extra)
     *      no descomprime gzip → quedás con bytes binarios en el cuerpo.
     *      El header "identity" significa "mandame el cuerpo sin compresión".
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(30).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new BrowserUserAgentInterceptor());
        return restTemplate;
    }

    /**
     * Interceptor que agrega headers de navegador a cada request del RestTemplate.
     * IMPORTANTE: usamos Accept-Encoding: identity para que Steam NO nos mande
     * la respuesta comprimida con gzip (el RestTemplate por default no descomprime).
     */
    private static class BrowserUserAgentInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution)
                throws IOException {
            request.getHeaders().set("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) " +
                    "Gecko/20100101 Firefox/131.0");
            request.getHeaders().set("Accept",
                    "application/json, text/plain, */*");
            request.getHeaders().set("Accept-Language", "en-US,en;q=0.9");
            // CRÍTICO: identity = sin compresión
            request.getHeaders().set("Accept-Encoding", "identity");
            return execution.execute(request, body);
        }
    }
}
