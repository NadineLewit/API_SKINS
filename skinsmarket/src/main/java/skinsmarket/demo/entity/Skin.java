package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Duration;
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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Double discount = 0.0;

    @Column(nullable = false)
    @JsonIgnore
    private Integer stock;

    @Column(nullable = false)
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_publicacion", length = 20)
    private EstadoPublicacion estadoPublicacion = EstadoPublicacion.PUBLICADA;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean intercambiable = true;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean vendible = true;

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

    /**
     * Item real del inventario de Steam que originó esta publicación.
     * Se mantiene oculto en JSON para no serializar todo el inventario/usuario.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventario_item_id", unique = true)
    @JsonIgnore
    private InventarioItem inventarioItem;

    /**
     * AssetID real en Steam. El bot lo necesita para entregar exactamente esta
     * skin cuando se venda. Las publicaciones admin pueden no tenerlo.
     */
    @Column(name = "steam_asset_id", length = 100)
    @JsonIgnore
    private String steamAssetId;

    /**
     * Momento en que asumimos que el bot recibió el item.
     * Mientras el bot real no esté activo, se setea al publicar para simular
     * el depósito correcto y poder arrancar el bloqueo de 7 días.
     */
    @Column(name = "bot_received_at")
    private LocalDateTime botReceivedAt;

    /**
     * Hasta cuándo la publicación se puede reservar, pero no entregar por Steam.
     * Representa el trade lock de Steam después de recibir el item.
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Enumerated(EnumType.STRING)
    private Rareza rareza;

    @Enumerated(EnumType.STRING)
    private Exterior exterior;

    @Column(nullable = false)
    private Boolean stattrak = false;

    @Transient
    private Double estimatedTradePrice;

    public Double getFinalPrice() {
        return price - (price * discount);
    }

    @JsonProperty("vendedorUsername")
    public String getVendedorUsername() {
        return vendedor != null ? vendedor.getRealUsername() : null;
    }

    @JsonProperty("locked")
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    @JsonProperty("secondsUntilUnlock")
    public long getSecondsUntilUnlock() {
        if (!isLocked()) return 0L;
        return Math.max(0L, Duration.between(LocalDateTime.now(), lockedUntil).getSeconds());
    }

    public enum Rareza {
        GRIS, CELESTE, AZUL, VIOLETA, ROSA, ROJO
    }

    public enum Exterior {
        RECIEN_FABRICADO, CASI_NUEVO, ALGO_DESGASTADO, BASTANTE_DESGASTADO, DEPLORABLE
    }

    public enum EstadoPublicacion {
        PUBLICADA,
        PAUSADA,
        RESERVADA,
        VENDIDA,
        ELIMINADA_ADMIN
    }
}
