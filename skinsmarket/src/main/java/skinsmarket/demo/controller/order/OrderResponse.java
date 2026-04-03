package skinsmarket.demo.controller.order;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Este objeto es lo que recibe el cliente al crear o consultar una orden.
 */
@Data
public class OrderResponse {

    // Identificador único de la orden
    private Long id;

    // Email del usuario que realizó la compra
    private String email;

    // Fecha y hora en que se creó la orden
    private LocalDateTime date;

    // Precio total original (sin descuentos)
    private Double totalPrice;

    // Porcentaje de descuento aplicado por cupón (ej: 0.15 = 15% off)
    // Será 0.0 si no se aplicó ningún cupón
    private Double descuentoAplicado;

    // Precio final después de aplicar el descuento del cupón
    private Double totalFinal;

    // Lista detallada de las skins compradas en esta orden
    private List<OrderDetailResponse> orderDetailResponses;
}
