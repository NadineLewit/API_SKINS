package skinsmarket.demo.controller.order;

import lombok.Data;

/**
 * DTO de respuesta para cada ítem dentro de una orden de compra.
 *
 * Se usa para mostrar el detalle de cada skin comprada en la respuesta de la orden.
 */
@Data
public class OrderDetailResponse {

    // ID de la skin comprada
    private Long skinId;

    // Nombre de la skin (para mostrar al usuario sin necesidad de otra consulta)
    private String skinName;

    // Cantidad de unidades compradas de esa skin
    private Integer quantity;

    // Precio unitario de la skin al momento de la compra
    // Se guarda el precio histórico para que no cambie si la skin se actualiza después
    private Double unitPrice;
}
