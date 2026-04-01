package skinsmarket.demo.dto;

import lombok.Data;
import skinsmarket.demo.entity.ItemCarrito;

import java.math.BigDecimal;

@Data
public class ItemCarritoResponse {

    private Long id;
    private Long skinId;
    private String skinNombre;
    private String skinImagenUrl;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public static ItemCarritoResponse fromEntity(ItemCarrito item) {
        ItemCarritoResponse dto = new ItemCarritoResponse();
        dto.setId(item.getId());
        dto.setSkinId(item.getSkin().getId());
        dto.setSkinNombre(item.getSkin().getNombre());
        dto.setSkinImagenUrl(item.getSkin().getImagenUrl());
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }
}
