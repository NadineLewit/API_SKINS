package skinsmarket.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class del marketplace de skins.
 *
 * CAMBIO: se agregó @EnableScheduling para activar el MockTradeScheduler que
 * simula al bot de Steam mientras la cuenta real está bloqueada hasta el 10/06.
 *
 * Cuando Steam habilite el trading, se puede apagar el mock con
 * mock.enabled=false en application.properties (no hace falta tocar código).
 */
@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(info = @Info(title = "Skinsmarket API", version = "1.0"))
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT"
)
public class SkinsmarketApplication {
	public static void main(String[] args) {
		SpringApplication.run(SkinsmarketApplication.class, args);
	}
}
