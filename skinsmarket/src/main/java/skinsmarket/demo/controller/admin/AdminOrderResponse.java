package skinsmarket.demo.controller.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para la vista de órdenes en el panel de administración.
 *
 * Idéntico al AdminOrderResponse del TPO aprobado.
 * Proporciona un resumen de cada orden de compra (sin exponer
 * detalles internos innecesarios).
 *
 * Usa @Builder para construcción limpia en el AdminController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderResponse {

    // Identificador único de la orden
    private Long id;

    // Email del usuario que realizó la compra (o "Usuario Eliminado" si fue borrado)
    private String userEmail;

    // Monto total de la orden (ya con descuento aplicado si hubo cupón)
    // Se usa BigDecimal para mayor precisión en cálculos monetarios
    private BigDecimal totalAmount;

    // Fecha y hora en que se realizó la compra
    private LocalDateTime creationDate;

    // Estado de la orden (ej: "PENDIENTE", "COMPLETADA", "CANCELADA")
    // Opcional: se puede usar null si el sistema no maneja estados de orden
    private String status;
}