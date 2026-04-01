package skinsmarket.demo.dto;

import lombok.Data;
import skinsmarket.demo.entity.Orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class OrdenResponse {

    private Long id;
    private String usuarioUsername;
    private LocalDateTime fecha;
    private String estado;
    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal total;
    private List<DetalleOrdenResponse> detalles;

    public static OrdenResponse fromEntity(Orden o) {
        OrdenResponse dto = new OrdenResponse();
        dto.setId(o.getId());
        dto.setUsuarioUsername(o.getUsuario().getUsername());
        dto.setFecha(o.getFecha());
        dto.setEstado(o.getEstado().name());
        dto.setSubtotal(o.getSubtotal());
        dto.setDescuentoTotal(o.getDescuentoTotal());
        dto.setTotal(o.getTotal());
        dto.setDetalles(
            o.getDetalles().stream()
                .map(DetalleOrdenResponse::fromEntity)
                .collect(Collectors.toList())
        );
        return dto;
    }
}
