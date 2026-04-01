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

    @Enumerated(EnumType.STRING)
    private Rareza rareza;

    @Enumerated(EnumType.STRING)
    private Exterior exterior;

    private Boolean stattrak = false;
    private Double descuento = 0.0;

    private LocalDateTime fechaAlta = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    public enum Rareza {
        GRIS,
        CELESTE,
        AZUL,
        VIOLETA,
        ROSA,
        ROJO
    }

    public enum Exterior {
        RECIEN_FABRICADO,
        CASI_NUEVO,
        ALGO_DESGASTADO,
        BASTANTE_DESGASTADO,
        DEPLORABLE
    }
}