package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad InventarioItem — representa un item del inventario de Steam
 * de un usuario, sincronizado desde la API pública de steamcommunity.com.
 *
 * Cada usuario puede tener múltiples InventarioItem. Cada uno corresponde
 * a un item físico real en su inventario de Steam, identificado por su
 * `assetId` (único en Steam — dos AK-47 Redline distintas tienen distinto assetId).
 *
 * RELACIÓN CON EL CATÁLOGO Y LAS PUBLICACIONES:
 *   - InventarioItem se cruza con SkinCatalogo por marketHashName cuando es posible
 *     (los nombres del catálogo ByMykel/CSGO-API matchean con los de Steam).
 *   - Si el usuario decide publicar un item a la venta, se crea una Skin
 *     (publicación) que referencia este InventarioItem y queda marcado como
 *     publicado=true para no permitir publicar el mismo asset dos veces.
 *
 * IMPORTANTE: este es un "espejo" del inventario real de Steam. Si el usuario
 * trade-ea un item afuera, el sync siguiente lo va a borrar/marcar inactivo.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventario_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "asset_id"}))
public class InventarioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario dueño del inventario (la skin pertenece a esta persona en Steam).
     * @JsonIgnore evita que al serializar el item se exponga toda la User
     * (con password y demás).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /**
     * AssetID de Steam — identificador único de este item físico en Steam.
     * Dos items aparentemente iguales (ej: dos AK-47 Redline) tienen assetId distintos.
     * Junto con user_id forma la unique constraint: un user no puede tener el mismo
     * assetId duplicado en la BD (evita duplicados al re-sincronizar).
     */
    @Column(name = "asset_id", nullable = false)
    private String assetId;

    /** ClassID de Steam — identifica la "clase" del item (todos los AK Redline tienen el mismo). */
    @Column(name = "class_id")
    private String classId;

    /** InstanceID de Steam — variante específica (incluye stickers, nametag, etc.). */
    @Column(name = "instance_id")
    private String instanceId;

    /**
     * Nombre canónico del item para matchear con el Steam Market y el catálogo.
     * Ejemplo: "AK-47 | Redline (Field-Tested)".
     */
    @Column(name = "market_hash_name", length = 300)
    private String marketHashName;

    /** Nombre legible para mostrar en UI (puede ser idéntico a marketHashName). */
    @Column(length = 300)
    private String name;

    /** URL de la imagen del item (servida por Steam CDN). */
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    /** Tipo del item según Steam (ej: "Classified Rifle", "Mil-Spec Grade Pistol"). */
    private String type;

    /** Si Steam permite tradear este item ahora mismo. */
    private Boolean tradable;

    /** Si el item es vendible en el Steam Market. */
    private Boolean marketable;

    /**
     * FK opcional al catálogo. Se completa cuando el marketHashName matchea con
     * un SkinCatalogo.name. Si no matchea (ej: cajas, llaves, stickers), queda null
     * pero el item igual se guarda — el usuario lo ve en su inventario aunque no
     * sea "publicable" en este marketplace.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalogo_id")
    private SkinCatalogo catalogo;

    /**
     * Si el usuario ya publicó este item a la venta en el marketplace.
     * Cuando es true, no se permite publicarlo de nuevo mientras la publicación
     * esté activa.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean publicado = false;

    /** Última vez que se sincronizó este item con Steam. */
    @Column(name = "fecha_sync")
    private LocalDateTime fechaSync;
}
