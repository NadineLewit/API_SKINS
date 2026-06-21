package skinsmarket.demo.controller.inventario;

import lombok.Data;
import skinsmarket.demo.entity.SkinCatalogo;

@Data
public class InventarioCatalogoResponse {

    private Long id;
    private String externalId;
    private String baseSkinId;
    private String name;
    private String marketHashName;
    private String description;
    private String imageUrl;
    private String weaponName;
    private String categoryName;
    private String exteriorName;
    private String rarezaName;
    private String rarezaColor;
    private Double minFloat;
    private Double maxFloat;
    private Boolean supportsStattrak;
    private Boolean supportsSouvenir;

    public static InventarioCatalogoResponse from(SkinCatalogo catalogo) {
        if (catalogo == null) return null;

        InventarioCatalogoResponse response = new InventarioCatalogoResponse();
        response.setId(catalogo.getId());
        response.setExternalId(catalogo.getExternalId());
        response.setBaseSkinId(catalogo.getBaseSkinId());
        response.setName(catalogo.getName());
        response.setMarketHashName(catalogo.getMarketHashName());
        response.setDescription(catalogo.getDescription());
        response.setImageUrl(catalogo.getImageUrl());
        response.setWeaponName(catalogo.getWeaponName());
        response.setCategoryName(catalogo.getCategoryName());
        response.setExteriorName(catalogo.getExteriorName());
        response.setRarezaName(catalogo.getRarezaName());
        response.setRarezaColor(catalogo.getRarezaColor());
        response.setMinFloat(catalogo.getMinFloat());
        response.setMaxFloat(catalogo.getMaxFloat());
        response.setSupportsStattrak(catalogo.getSupportsStattrak());
        response.setSupportsSouvenir(catalogo.getSupportsSouvenir());
        return response;
    }
}
