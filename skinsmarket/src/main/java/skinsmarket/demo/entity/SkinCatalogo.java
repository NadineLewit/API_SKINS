package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad SkinCatalogo — catálogo maestro de skins reales de CS2.
 *
 * Cada registro corresponde a un modelo de skin del juego (ej "AK-47 | Redline"),
 * sin importar quién lo posea. Se importa desde la API de ByMykel/CSGO-API.
 *
 * Es la fuente de verdad para nombres, descripciones e imágenes oficiales.
 * Las publicaciones de venta (Skin) referencian este catálogo y heredan los
 * datos automáticamente.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "skin_catalogo")
public class SkinCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID externo de ByMykel (ej: "skin-1"). Único, sirve para upsert. */
    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    /** Nombre canónico de la skin (ej: "AK-47 | Redline"). */
    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 1000)
    private String description;

    /** URL pública de la imagen de Steam CDN. */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Arma a la que pertenece (ej: "AK-47"). */
    @Column(name = "weapon_name", length = 100)
    private String weaponName;

    /** Categoría (ej: "Rifle", "Knife", "Pistol"). */
    @Column(name = "category_name", length = 100)
    private String categoryName;

    /** Nombre de la rareza en inglés (ej: "Classified", "Covert"). */
    @Column(name = "rareza_name", length = 50)
    private String rarezaName;

    /** Color hex de la rareza (ej: "#d32ce6"). */
    @Column(name = "rareza_color", length = 20)
    private String rarezaColor;

    /** Float mínimo posible para esta skin (0.0 a 1.0). */
    @Column(name = "min_float")
    private Double minFloat;

    /** Float máximo posible. */
    @Column(name = "max_float")
    private Double maxFloat;

    /** Si soporta versión StatTrak. */
    @Column(name = "supports_stattrak")
    private Boolean supportsStattrak;

    /** Si soporta versión Souvenir. */
    @Column(name = "supports_souvenir")
    private Boolean supportsSouvenir;
}
