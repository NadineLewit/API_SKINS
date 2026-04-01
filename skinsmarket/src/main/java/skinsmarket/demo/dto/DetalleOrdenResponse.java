package skinsmarket.demo.dto;

import lombok.Data;
import skinsmarket.demo.entity.DetalleOrden;

import java.math.BigDecimal;

@Data
public class DetalleOrdenResponse {

    private Long id;
    private Long skinId;
    private String skinNombre;
    private String skinImagenUrl;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public static DetalleOrdenResponse fromEntity(DetalleOrden d) {
        DetalleOrdenResponse dto = new DetalleOrdenResponse();
        dto.setId(d.getId());
        dto.setSkinId(d.getSkin().getId());
        dto.setSkinNombre(d.getSkin().getNombre());
        dto.setSkinImagenUrl(d.getSkin().getImagenUrl());
        dto.setCantidad(d.getCantidad());
        dto.setPrecioUnitario(d.getPrecioUnitario());
        dto.setSubtotal(d.getSubtotal());
        return dto;
    }
}
