package skinsmarket.demo.dto;

import lombok.Data;
import skinsmarket.demo.entity.Skin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SkinResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private BigDecimal precioFinal;
    private Integer stock;
    private Boolean activa;
    private String imagenUrl;
    private String categoria;
    private String rareza;
    private String exterior;
    private Boolean stattrak;
    private Double descuento;
    private LocalDateTime fechaAlta;
    private String vendedorUsername;

    public static SkinResponse fromEntity(Skin s) {
        SkinResponse dto = new SkinResponse();
        dto.setId(s.getId());
        dto.setNombre(s.getNombre());
        dto.setDescripcion(s.getDescripcion());
        dto.setPrecio(s.getPrecio());
        dto.setPrecioFinal(calcularPrecioFinal(s));
        dto.setStock(s.getStock());
        dto.setActiva(s.getActiva());
        dto.setImagenUrl(s.getImagenUrl());
        dto.setCategoria(s.getCategoria() != null ? s.getCategoria().getNombre() : null);
        dto.setRareza(s.getRareza() != null ? s.getRareza().name() : null);
        dto.setExterior(s.getExterior() != null ? s.getExterior().name() : null);
        dto.setStattrak(s.getStattrak());
        dto.setDescuento(s.getDescuento());
        dto.setFechaAlta(s.getFechaAlta());
        dto.setVendedorUsername(s.getVendedor() != null ? s.getVendedor().getUsername() : null);
        return dto;
    }

    private static BigDecimal calcularPrecioFinal(Skin s) {
        if (s.getDescuento() == null || s.getDescuento() == 0.0) return s.getPrecio();
        BigDecimal descuentoDecimal = BigDecimal.valueOf(s.getDescuento() / 100);
        return s.getPrecio().subtract(s.getPrecio().multiply(descuentoDecimal));
    }
}
