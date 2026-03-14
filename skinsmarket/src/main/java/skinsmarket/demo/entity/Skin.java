package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "skins")
public class Skin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private BigDecimal precio;

    @Column(nullable = false)
    private Integer stock;

    private Boolean activa = true;

    private String imagenUrl;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    private String rareza;
    private String exterior;
    private String coleccion;
    private Boolean stattrak = false;
    private Double descuento = 0.0;

    private LocalDateTime fechaAlta = LocalDateTime.now();
}
