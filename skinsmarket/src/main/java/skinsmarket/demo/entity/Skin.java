package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entidad que representa una Skin (artículo cosmético de videojuego) en el marketplace.
 *
 * Reemplaza la entidad Game del TPO aprobado, manteniendo su estructura base
 * (id, price, discount, stock, imageUrl, category) y agregando atributos
 * propios del dominio de skins: rareza, exterior, stattrak, vendedor y estado activo.
 *
 * Relaciones:
 *   - Una skin pertenece a una categoría (ManyToOne → Category)
 *   - Una skin tiene un vendedor/publicador (ManyToOne → User)
 *   - Una skin puede aparecer en muchos detalles de orden (OneToMany ← OrderDetail)
 *   - Una skin puede aparecer en muchos ítems de carrito (OneToMany ← ItemCarrito)
 */
@Entity
@Data
@Table(name = "skins")
public class Skin {

    // Identificador único generado automáticamente por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre de la skin (ej: "AK-47 | Redline", "AWP | Dragon Lore")
    @Column(nullable = false)
    private String name;

    // Descripción opcional de la skin
    @Column(length = 500)
    private String description;

    // Precio base de la skin (equivalente a price en Game del TPO)
    @Column(nullable = false)
    private Double price;

    // Descuento aplicado sobre el precio base (valor entre 0.0 y 1.0)
    // Ej: 0.10 = 10% de descuento. Por defecto no hay descuento.
    @Column(nullable = false)
    private Double discount = 0.0;

    // Stock disponible para la venta (equivalente al stock de Game en el TPO)
    @Column(nullable = false)
    private Integer stock;

    // Indica si la skin está activa/disponible para compra (baja lógica)
    // false = la skin fue eliminada pero se mantiene el registro histórico
    @Column(nullable = false)
    private Boolean active = true;

    // URL pública de la imagen de la skin (generada al subir el archivo)
    @Column(name = "image_url")
    private String imageUrl;

    // Nombre del videojuego al que pertenece la skin (ej: "CS2", "Dota 2", "TF2")
    @Column(nullable = false)
    private String game;

    // Fecha y hora en que la skin fue publicada en el marketplace
    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Relaciones con otras entidades
    // -------------------------------------------------------------------------

    // Categoría a la que pertenece la skin (ManyToOne: muchas skins → una categoría)
    // Equivalente a la relación ManyToMany con Category en Game del TPO,
    // simplificada a ManyToOne para el dominio de skins
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // Usuario que publicó la skin (vendedor)
    // Una misma skin solo puede tener un vendedor/publicador
    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    private User vendedor;

    // -------------------------------------------------------------------------
    // Atributos específicos del dominio de skins
    // -------------------------------------------------------------------------

    // Rareza visual de la skin (determina su valor y escasez en el mercado)
    @Enumerated(EnumType.STRING)
    private Rareza rareza;

    // Estado de desgaste del exterior de la skin
    @Enumerated(EnumType.STRING)
    private Exterior exterior;

    // Indica si la skin es StatTrak™ (contador de kills integrado)
    // Las versiones StatTrak son más raras y valen más
    @Column(nullable = false)
    private Boolean stattrak = false;

    // -------------------------------------------------------------------------
    // Método utilitario
    // -------------------------------------------------------------------------

    /**
     * Calcula el precio final de la skin aplicando el descuento.
     * Equivalente al método getFinalPrice() de Game en el TPO aprobado.
     *
     * @return precio base menos el porcentaje de descuento
     */
    public Double getFinalPrice() {
        return price - (price * discount);
    }

    // -------------------------------------------------------------------------
    // Enums internos del dominio de skins
    // -------------------------------------------------------------------------

    /**
     * Rareza de la skin, de menor a mayor valor:
     *   GRIS    → Consumer Grade (más común)
     *   CELESTE → Industrial Grade
     *   AZUL    → Mil-Spec
     *   VIOLETA → Restricted
     *   ROSA    → Classified
     *   ROJO    → Covert (más raro)
     */
    public enum Rareza {
        GRIS,
        CELESTE,
        AZUL,
        VIOLETA,
        ROSA,
        ROJO
    }

    /**
     * Estado de desgaste del exterior de la skin (de mejor a peor):
     *   RECIEN_FABRICADO    → Factory New (FN)
     *   CASI_NUEVO          → Minimal Wear (MW)
     *   ALGO_DESGASTADO     → Field-Tested (FT)
     *   BASTANTE_DESGASTADO → Well-Worn (WW)
     *   DEPLORABLE          → Battle-Scarred (BS)
     */
    public enum Exterior {
        RECIEN_FABRICADO,
        CASI_NUEVO,
        ALGO_DESGASTADO,
        BASTANTE_DESGASTADO,
        DEPLORABLE
    }
}
