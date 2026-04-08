package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Entidad Skin — marketplace de skins de videojuegos.
 *
 * CAMBIO (pedido por la profe):
 *   - Se reemplaza imageUrl (String) por image (byte[]) almacenado como BLOB en la BD.
 *   - Al devolver la skin, la imagen se serializa en base64 para que el frontend
 *     la renderice con: <img src={`data:image/jpeg;base64,${skin.imageBase64}`} />
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
    // Imagen almacenada como BLOB en la BD
    // -------------------------------------------------------------------------

    /**
     * Bytes de la imagen de la skin almacenados en la base de datos.
     *
     * @Lob indica a Hibernate que este campo se almacena como BLOB (Binary Large Object).
     * columnDefinition = "LONGBLOB" permite imágenes de hasta 4GB en MySQL.
     *
     * Para subirla: multipart/form-data con campo "image" (MultipartFile).
     * Para mostrarla: el frontend usa imageBase64 que devuelve este campo en base64.
     */
    @Lob
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
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User vendedor;

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

    /**
     * Precio final aplicando el descuento de la skin.
     * Igual que getFinalPrice() del TPO aprobado.
     */
    public Double getFinalPrice() {
        return price - (price * discount);
    }

    /**
     * Devuelve la imagen codificada en base64 para que el frontend la renderice.
     *
     * Uso en el frontend:
     *   <img src={`data:image/jpeg;base64,${skin.imageBase64}`} />
     *
     * Devuelve null si no hay imagen cargada.
     */
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
