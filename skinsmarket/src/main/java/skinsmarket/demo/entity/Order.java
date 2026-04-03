package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una Orden de compra de skins.
 *
 * Relaciones:
 *   - Muchas órdenes pertenecen a un usuario (ManyToOne → User)
 *   - Una orden tiene muchos detalles de compra (OneToMany → OrderDetail)
 *
 * Se usa @Table(name="orders") para evitar conflicto con la palabra reservada
 * ORDER en SQL
 */
@Entity
@Data
@Table(name = "orders")
public class Order {

    // Identificador único generado automáticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario que realizó la compra
    // FK: user_id referencia a la tabla users
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Fecha y hora en que se creó la orden
    @Column(nullable = false)
    private LocalDateTime date;

    // Precio total original de la orden (sin descuentos)
    // Suma de (precio unitario × cantidad) de todos los detalles
    @Column(nullable = false)
    private Double totalPrice;

    // Porcentaje de descuento aplicado por cupón (0.0 si no se usó ninguno)
    // Ej: 0.15 equivale a un 15% de descuento
    @Column(nullable = false)
    private Double descuentoAplicado = 0.0;

    // Precio final tras aplicar el descuento del cupón
    // totalFinal = totalPrice * (1 - descuentoAplicado)
    // Default 0.0 para evitar NPE al leer órdenes existentes sin este campo
    @Column(nullable = false)
    private Double totalFinal = 0.0;

    // Lista de detalles de la orden (cada ítem skin + cantidad + precio unitario)
    // CascadeType.ALL: al persistir/eliminar la orden, se persisten/eliminan sus detalles
    // orphanRemoval = true: si un detalle se quita de la lista, se elimina de la BD
    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderDetail> orderDetails = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Métodos auxiliares de gestión de detalles (idénticos al TPO aprobado)
    // -------------------------------------------------------------------------

    /**
     * Agrega un detalle a la orden y establece la referencia bidireccional.
     * Mantiene sincronizados ambos lados de la relación OneToMany.
     *
     * @param d detalle de orden a agregar
     */
    public void addOrderDetail(OrderDetail d) {
        orderDetails.add(d);
        d.setOrder(this); // Mantener la FK en el detalle apuntando a esta orden
    }

    /**
     * Quita un detalle de la orden y limpia la referencia bidireccional.
     *
     * @param d detalle de orden a quitar
     */
    public void removeOrderDetail(OrderDetail d) {
        orderDetails.remove(d);
        d.setOrder(null); // Limpiar la FK para que JPA haga orphanRemoval
    }
}