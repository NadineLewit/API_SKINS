package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Entidad Skin — representa una PUBLICACIÓN de venta en el marketplace.
 *
 * IMPORTANTE: una Skin NO es lo mismo que el item del juego. Es una OFERTA de
 * un vendedor (USER o ADMIN) sobre un item del catálogo (SkinCatalogo).
 *   - Un USER solo puede publicar Skin si referencia un SkinCatalogo existente.
 *   - Un ADMIN puede publicar libre (catalogo = null) o sobre el catálogo.
 *
 * La imagen se almacena como BLOB pero está marcada con @JsonIgnore para no
 * inundar las respuestas de Insomnia/Postman con base64. Si el frontend la
 * necesita, se puede exponer un endpoint dedicado o usar la imageUrl del
 * SkinCatalogo asociado.
 */
@Entity
@Data
@Table(name = "skins")
public class Skin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Double discount = 0.0;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private String game;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Imagen almacenada como BLOB en la BD (oculta del JSON)
    // -------------------------------------------------------------------------

    @Lob
    @JsonIgnore
    @Column(name = "image", columnDefinition = "LONGBLOB")
    private byte[] image;

    // -------------------------------------------------------------------------
    // Relaciones
    // -------------------------------------------------------------------------

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    @JsonIgnore
    private User vendedor;

    /**
     * Referencia al item del catálogo de skins reales.
     *
     * - OBLIGATORIA cuando un USER publica (validado en el service).
     * - OPCIONAL cuando un ADMIN publica.
     *
     * Permite asociar la publicación de venta con la skin oficial del juego
     * y heredar sus atributos (nombre, descripción, imagen, rareza, etc.).
     */
    @ManyToOne
    @JoinColumn(name = "catalogo_id")
    private SkinCatalogo catalogo;

    // -------------------------------------------------------------------------
    // Atributos del dominio de skins
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    private Rareza rareza;

    @Enumerated(EnumType.STRING)
    private Exterior exterior;

    @Column(nullable = false)
    private Boolean stattrak = false;

    // -------------------------------------------------------------------------
    // Métodos utilitarios
    // -------------------------------------------------------------------------

    /** Precio final aplicando el descuento de la skin. */
    public Double getFinalPrice() {
        return price - (price * discount);
    }

    /** Imagen en base64 — oculta del JSON con @JsonIgnore. */
    @JsonIgnore
    public String getImageBase64() {
        if (image == null) return null;
        return Base64.getEncoder().encodeToString(image);
    }

    // -------------------------------------------------------------------------
    // Enums
    // -------------------------------------------------------------------------

    public enum Rareza {
        GRIS, CELESTE, AZUL, VIOLETA, ROSA, ROJO
    }

    public enum Exterior {
        RECIEN_FABRICADO, CASI_NUEVO, ALGO_DESGASTADO, BASTANTE_DESGASTADO, DEPLORABLE
    }
}
