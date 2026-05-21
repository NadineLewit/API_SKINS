package skinsmarket.demo.controller.skincatalogo;

import lombok.Data;

@Data
public class SkinCatalogoMarketStatsResponse {
    private Long catalogoId;
    private String name;
    private String marketHashName;
    private String weaponName;
    private String categoryName;
    private String exteriorName;
    private String imageUrl;
    private Boolean supportsStattrak;

    private Integer stock;
    private Double precioPromedio;
    private Double precioMinimo;
    private Double precioMaximo;

    private Integer stockStattrak;
    private Double precioPromedioStattrak;
    private Double precioMinimoStattrak;
    private Double precioMaximoStattrak;
}
