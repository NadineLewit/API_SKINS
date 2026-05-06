package skinsmarket.demo.controller.skincatalogo;

import lombok.Data;

/**
 * DTO para crear items del catálogo manualmente (solo ADMIN).
 *
 * En el flujo normal el catálogo se importa automáticamente desde
 * la API de ByMykel (CommandLineRunner CatalogoSeeder). Este DTO se usa
 * solo cuando el ADMIN quiere agregar una skin a mano que no está en la
 * API pública (caso muy raro).
 */
@Data
public class SkinCatalogoRequest {

    /** ID externo (ej: el de ByMykel). Si lo creás manual, podés usar prefijo "manual_". */
    private String externalId;

    private String name;
    private String description;
    private String imageUrl;
    private String weaponName;
    private String categoryName;
    private String rarezaName;
    private String rarezaColor;
    private Double minFloat;
    private Double maxFloat;
    private Boolean supportsStattrak;
    private Boolean supportsSouvenir;
}
