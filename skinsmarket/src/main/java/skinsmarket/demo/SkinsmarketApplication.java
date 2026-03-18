package skinsmarket.demo;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// http://localhost:8080/swagger-ui/index.html

@SpringBootApplication
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