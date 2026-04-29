package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad SkinCatalogo — catálogo maestro de skins reales del juego.
 *
 * Esta es la "base de datos de skins" que sirve como referencia. Las skins
 * de venta (entidad Skin) se publican siempre referenciando a un SkinCatalogo.
 *
 * Datos importados desde la API pública de ByMykel/CSGO-API:
 *   https://bymykel.github.io/CSGO-API/api/en/skins.json
 *
 * Quién puede crear catálogo:
 *   - ADMIN: puede crear, importar (sincronizar desde la API) o eliminar.
 *   - USER: solo puede consultar el catálogo (read-only).
 *
 * Quién puede publicar a partir del catálogo:
 *   - USER: ÚNICAMENTE puede publicar Skin que apunten a un catálogo existente.
 *   - ADMIN: puede publicar con o sin catálogo.
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

    /**
     * ID externo de la API de Steam (ej: "skin-65604").
     * Permite identificar la skin de forma única en la fuente de datos
     * y evita duplicados al sincronizar.
     */
    @Column(name = "external_id", unique = true)
    private String externalId;

    /** Nombre completo de la skin (ej: "AK-47 | Redline"). */
    @Column(nullable = false)
    private String name;

    /** Descripción del lore/historia del item según el juego. */
    @Column(length = 2000)
    private String description;

    /** Nombre del arma (ej: "AK-47", "Desert Eagle"). */
    @Column(name = "weapon_name")
    private String weaponName;

    /** Nombre de la categoría según la API (ej: "Rifles", "Pistols", "Knives"). */
    @Column(name = "category_name")
    private String categoryName;

    /** Nombre de la rareza (ej: "Mil-Spec Grade", "Covert"). */
    @Column(name = "rareza_name")
    private String rarezaName;

    /** Color de la rareza en hex (ej: "#4b69ff"). Útil para UI. */
    @Column(name = "rareza_color")
    private String rarezaColor;

    /** URL de la imagen oficial de la skin (servida desde GitHub). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Float mínimo permitido para esta skin. */
    @Column(name = "min_float")
    private Double minFloat;

    /** Float máximo permitido para esta skin. */
    @Column(name = "max_float")
    private Double maxFloat;

    /** Si la skin admite la variante StatTrak. */
    @Column(name = "supports_stattrak")
    private Boolean supportsStattrak;

    /** Si la skin admite la variante Souvenir. */
    @Column(name = "supports_souvenir")
    private Boolean supportsSouvenir;
}
