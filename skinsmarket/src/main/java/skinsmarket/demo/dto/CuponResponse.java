package skinsmarket.demo.dto;

import lombok.Data;
import skinsmarket.demo.entity.Cupon;

import java.time.LocalDate;

@Data
public class CuponResponse {

    private Long id;
    private String codigo;
    private Double porcentaje;
    private Boolean activo;
    private LocalDate fechaVencimiento;

    public static CuponResponse fromEntity(Cupon c) {
        CuponResponse dto = new CuponResponse();
        dto.setId(c.getId());
        dto.setCodigo(c.getCodigo());
        dto.setPorcentaje(c.getPorcentaje());
        dto.setActivo(c.getActivo());
        dto.setFechaVencimiento(c.getFechaVencimiento());
        return dto;
    }
}
