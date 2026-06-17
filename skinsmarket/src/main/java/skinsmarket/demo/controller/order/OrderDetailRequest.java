package skinsmarket.demo.controller.order;

import lombok.Data;

/**
 * DTO para cada ítem dentro de una solicitud de orden.
 *
 */
@Data
public class OrderDetailRequest {

    // ID de la skin que se desea comprar
    private Long skinId;

    // Cantidad de unidades de esa skin a comprar
    private Integer quantity;
}