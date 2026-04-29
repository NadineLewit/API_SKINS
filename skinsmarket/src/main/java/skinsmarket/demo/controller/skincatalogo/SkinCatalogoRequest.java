package skinsmarket.demo.controller.skincatalogo;

import lombok.Data;

/**
 * DTO para creación manual de un item del catálogo de skins.
 *
 * Este endpoint es para casos especiales donde el ADMIN quiere registrar
 * una skin que no está en la API pública (por ejemplo, una skin custom).
 * El flujo normal es usar POST /catalogo/sincronizar para importar desde la API.
 */
@Data
public class SkinCatalogoRequest {

    /**
     * ID externo. Si se crea manualmente, podés usar cualquier identificador único
     * (ej: "custom-001"). Si lo dejás null, la BD asigna solo el id interno.
     */
    private String externalId;

    private String name;
    private String description;
    private String weaponName;
    private String categoryName;
    private String rarezaName;
    private String rarezaColor;
    private String imageUrl;
    private Double minFloat;
    private Double maxFloat;
    private Boolean supportsStattrak;
    private Boolean supportsSouvenir;
}
