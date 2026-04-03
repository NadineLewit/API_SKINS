package skinsmarket.demo.controller.cupon;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO para las solicitudes de creación de Cupones de descuento.
 *
 * Sigue el mismo patrón de los DTOs de request del TPO aprobado.
 * Transporta los datos necesarios para crear un nuevo cupón.
 */
@Data
public class CuponRequest {

    // Código alfanumérico único del cupón (ej: "SUMMER20", "BLACKFRIDAY")
    private String codigo;

    // Porcentaje de descuento que aplica el cupón (ej: 0.15 = 15% de descuento)
    private Double descuento;

    // Fecha y hora de expiración del cupón (null = sin vencimiento)
    private LocalDateTime fechaExpiracion;

    // Indica si el cupón puede ser usado más de una vez (false = uso único)
    private Boolean multiUso;
}