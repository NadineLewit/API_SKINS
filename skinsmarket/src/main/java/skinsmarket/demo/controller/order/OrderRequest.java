package skinsmarket.demo.controller.order;

import lombok.Data;

import java.util.List;

/**
 * El cliente envía la lista de items de la orden y, opcionalmente, un código de cupón.
 */
@Data
public class OrderRequest {

    // Lista de items de la orden (cada item contiene skinId y cantidad)
    private List<OrderDetailRequest> itemList;

    // Código de cupón de descuento (opcional, puede ser null si no se aplica ninguno)
    // Ejemplo: "SUMMER20", "BLACKFRIDAY"
    private String codigoCupon;
}