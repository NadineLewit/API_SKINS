package skinsmarket.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de recursos estáticos para Spring MVC.
 *
 * Idéntica al WebConfig del TPO aprobado.
 * Permite que las imágenes de skins subidas al servidor sean accesibles
 * públicamente desde la URL /uploads/{filename}.
 *
 * Sin esta configuración, Spring bloquearía las peticiones a /uploads/**
 * porque no son endpoints REST definidos en ningún @RestController.
 *
 * Ejemplo de uso:
 *   - Al crear una skin con imagen, el SkinController guarda el archivo en
 *     la carpeta local "uploads/" y genera la URL:
 *     http://localhost:4002/uploads/1234567890_dragon_lore.png
 *   - Esta URL es la que se almacena en Skin.imageUrl
 *   - El frontend la usa directamente para mostrar la imagen al usuario
 *
 * La carpeta "uploads/" se crea automáticamente en la raíz del proyecto
 * (mismo nivel que src/ y pom.xml) cuando se sube la primera imagen.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registra el handler de recursos estáticos para la carpeta de uploads.
     *
     * /uploads/**  → cualquier URL que empiece con /uploads/ es mapeada a
     * file:uploads/ → la carpeta local "uploads/" en la raíz del proyecto.
     *
     * El prefijo "file:" indica que es una ruta del sistema de archivos local,
     * a diferencia de "classpath:" que buscaría dentro del JAR.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
