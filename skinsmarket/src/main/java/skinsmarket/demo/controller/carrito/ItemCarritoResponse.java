package skinsmarket.demo.controller.carrito;

import lombok.Data;
import skinsmarket.demo.entity.ItemCarrito;
import skinsmarket.demo.entity.Skin;

@Data
public class ItemCarritoResponse {

    private Long id;
    private Skin skin;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    public static ItemCarritoResponse from(ItemCarrito item) {
        ItemCarritoResponse response = new ItemCarritoResponse();
        response.setId(item.getId());
        response.setSkin(item.getSkin());
        response.setCantidad(item.getCantidad());
        response.setPrecioUnitario(item.getPrecioUnitario());
        response.setSubtotal(item.getSubtotal());
        return response;
    }
}
