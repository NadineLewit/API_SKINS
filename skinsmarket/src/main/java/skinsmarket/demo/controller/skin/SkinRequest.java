package skinsmarket.demo.controller.skin;

import lombok.Data;

/**
 * DTO para crear/editar Skins.
 *
 * Si se manda catalogoId: name/description/imageUrl se IGNORAN del request
 * y se copian automáticamente del catálogo. La "categoría" también sale del
 * catálogo (catalogo.categoryName).
 *
 * Si NO se manda catalogoId (solo ADMIN): se usan los valores del request.
 */
@Data
public class SkinRequest {

    /** ID del catálogo. Para ADMIN es opcional, para USER es obligatorio. */
    private Long catalogoId;

    private String name;
    private String description;
    private Double price;
    private Double discount;
    private String game;

    /**
     * URL pública de la imagen. Si se manda catalogoId, este campo se ignora
     * y se copia del catálogo automáticamente.
     */
    private String imageUrl;

    private Integer stock;
    private String rareza;
    private String exterior;
    private Boolean stattrak;
}
