package skinsmarket.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Entidad que representa un Cupón de descuento.
 *
 * Entidad nueva respecto al TPO aprobado: el marketplace de skins agrega
 * la funcionalidad de cupones para promociones y descuentos especiales.
 * Sigue la misma estructura JPA de las demás entidades del proyecto.
 *
 * Un cupón tiene un código único, un porcentaje de descuento y puede tener
 * fecha de vencimiento. Puede ser de uso único o múltiple.
 *
 * Relaciones: no tiene relaciones directas con otras entidades.
 * El cupón se valida por código en el momento de la compra (ver OrderServiceImpl).
 */
@Entity
@Data
@Table(name = "cupones")
public class Cupon {

    // Identificador único generado automáticamente
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código alfanumérico único del cupón (ej: "SUMMER20", "BLACKFRIDAY50")
    // nullable = false: el código es obligatorio
    // unique = true: no pueden existir dos cupones con el mismo código
    @Column(nullable = false, unique = true)
    private String codigo;

    // Porcentaje de descuento que aplica este cupón (valor entre 0.0 y 1.0)
    // Ej: 0.20 = 20% de descuento sobre el total de la orden
    @Column(nullable = false)
    private Double descuento;

    // Indica si el cupón está activo y puede ser utilizado
    // Un admin puede desactivarlo aunque no haya vencido (activo = false)
    @Column(nullable = false)
    private Boolean activo = true;

    // Fecha de vencimiento del cupón (null = sin fecha de vencimiento)
    // Si la fecha actual es posterior a fechaVencimiento, el cupón no es válido
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    // Indica si el cupón puede ser usado más de una vez (true) o solo una vez (false)
    // Los cupones de uso único se desactivan automáticamente tras su primer uso
    @Column(nullable = false)
    private Boolean multiUso = false;
}
