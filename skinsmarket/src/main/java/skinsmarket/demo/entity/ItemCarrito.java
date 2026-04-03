package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidad que representa un ítem (línea) dentro del Carrito de compras.
 *
 *
 * Guarda la skin elegida, la cantidad deseada y el precio unitario
 * en el momento en que se agregó al carrito (snapshot de precio).
 *
 * Relaciones:
 *   - Muchos ítems pertenecen a un carrito (ManyToOne → Carrito)
 *   - Cada ítem referencia a una skin (ManyToOne → Skin)
 */
@Entity
@Data
@Table(name = "items_carrito")
public class ItemCarrito {

    // Identificador único generado automáticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Carrito al que pertenece este ítem
    // FK: carrito_id referencia a la tabla carritos
    @ManyToOne
    @JoinColumn(name = "carrito_id")
    private Carrito carrito;

    // Skin que el usuario desea comprar
    // FK: skin_id referencia a la tabla skins
    @ManyToOne
    @JoinColumn(name = "skin_id")
    private Skin skin;

    // Cantidad de unidades de esta skin en el carrito
    // Puede modificarse con PUT /carrito/items/{itemId}?cantidad=N
    @Column(nullable = false)
    private Integer cantidad;

    // Precio unitario de la skin al momento de agregarla al carrito
    // Se usa para mostrar el total del carrito y detectar si el precio cambió
    @Column(nullable = false)
    private Double precioUnitario;

    // -------------------------------------------------------------------------
    // Método utilitario
    // -------------------------------------------------------------------------

    /**
     * Calcula el subtotal de este ítem (precio unitario × cantidad).
     * Usado en el CarritoService para calcular el total del carrito.
     *
     * @return subtotal del ítem
     */
    public Double getSubtotal() {
        return precioUnitario * cantidad;
    }
}
