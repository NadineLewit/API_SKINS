package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Entidad que representa el detalle de una línea dentro de una Orden de compra.
 *
 * Representa cada skin comprada dentro de una orden, junto con la
 * cantidad adquirida y el precio unitario histórico (congelado al momento
 * de la compra, para que no cambie si el precio de la skin se actualiza después).
 *
 * Relaciones:
 *   - Muchos detalles pertenecen a una orden (ManyToOne → Order)
 *   - Muchos detalles referencian a una skin (ManyToOne → Skin)
 */
@Entity
@Data
@Table(name = "order_details")
public class OrderDetail {

    // Identificador único generado automáticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Orden a la que pertenece este detalle
    // FK: order_id referencia a la tabla orders
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Skin comprada en esta línea de detalle
    // FK: skin_id referencia a la tabla skins
    // Equivalente a game en el OrderDetail del TPO aprobado
    @ManyToOne
    @JoinColumn(name = "skin_id")
    private Skin skin;

    // Cantidad de unidades de esta skin compradas en la orden
    @Column(nullable = false)
    private Integer quantity;

    // Precio unitario de la skin al momento de la compra (precio histórico)
    // Se almacena para que el historial no se vea afectado por cambios de precio futuros
    @Column(nullable = false)
    private Double unitPrice;
}
