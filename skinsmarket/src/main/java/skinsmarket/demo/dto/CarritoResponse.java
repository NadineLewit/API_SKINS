package skinsmarket.demo.dto;

import lombok.Data;
import skinsmarket.demo.entity.Carrito;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class CarritoResponse {

    private Long id;
    private String estado;
    private List<ItemCarritoResponse> items;
    private BigDecimal total;

    public static CarritoResponse fromEntity(Carrito c) {
        CarritoResponse dto = new CarritoResponse();
        dto.setId(c.getId());
        dto.setEstado(c.getEstado().name());
        dto.setItems(
            c.getItems().stream()
                .map(ItemCarritoResponse::fromEntity)
                .collect(Collectors.toList())
        );
        dto.setTotal(
            c.getItems().stream()
                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        return dto;
    }
}
