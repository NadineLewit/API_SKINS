package skinsmarket.demo.controller.carrito;

import lombok.Data;
import skinsmarket.demo.entity.Carrito;

import java.util.List;

@Data
public class CarritoResponse {

    private Long id;
    private Carrito.Estado estado;
    private List<ItemCarritoResponse> items;
    private Double total;

    public static CarritoResponse from(Carrito carrito) {
        CarritoResponse response = new CarritoResponse();
        List<ItemCarritoResponse> items = carrito.getItems().stream()
                .map(ItemCarritoResponse::from)
                .toList();

        response.setId(carrito.getId());
        response.setEstado(carrito.getEstado());
        response.setItems(items);
        response.setTotal(items.stream()
                .mapToDouble(item -> item.getSubtotal() != null ? item.getSubtotal() : 0.0)
                .sum());
        return response;
    }
}
