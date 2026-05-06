package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad Skin — representa una PUBLICACIÓN de venta en el marketplace.
 *
 * CAMBIOS:
 *   - Sin BLOB de imagen: usa imageUrl (string) que viene del catálogo de ByMykel.
 *   - Sin relación con Category: la "categoría" ahora se deriva del catálogo
 *     asociado (catalogo.categoryName: "Rifle", "Pistol", "Knife", etc).
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

    /**
     * URL pública de la imagen de la skin. Idealmente viene del catálogo
     * de ByMykel/CSGO-API.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    @JsonIgnore
    private User vendedor;

    /**
     * Referencia al item del catálogo. La "categoría" sale de acá:
     * skin.catalogo.categoryName
     */
    @ManyToOne
    @JoinColumn(name = "catalogo_id")
    private SkinCatalogo catalogo;

    @Enumerated(EnumType.STRING)
    private Rareza rareza;

    @Enumerated(EnumType.STRING)
    private Exterior exterior;

    @Column(nullable = false)
    private Boolean stattrak = false;

    public Double getFinalPrice() {
        return price - (price * discount);
    }

    public enum Rareza {
        GRIS, CELESTE, AZUL, VIOLETA, ROSA, ROJO
    }

    public enum Exterior {
        RECIEN_FABRICADO, CASI_NUEVO, ALGO_DESGASTADO, BASTANTE_DESGASTADO, DEPLORABLE
    }
}
