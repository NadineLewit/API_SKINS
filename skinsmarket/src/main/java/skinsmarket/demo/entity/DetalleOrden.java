package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "detalle_orden")
public class DetalleOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "orden_id")
    private Orden orden;

    @ManyToOne
    @JoinColumn(name = "skin_id")
    private Skin skin;

    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}