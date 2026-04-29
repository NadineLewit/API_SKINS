package skinsmarket.demo.controller.skin;

import lombok.Data;

/**
 * DTO para las solicitudes de creación y edición de Skins (publicaciones de venta).
 *
 * NO incluye id: en creación JPA lo genera automáticamente.
 *
 * REGLA DE NEGOCIO:
 *   - USER (vendedor): catalogoId es OBLIGATORIO. La publicación queda atada a
 *     un item del catálogo y los atributos visuales (name, description, etc.)
 *     se completan automáticamente desde ese catálogo, ignorando lo que el
 *     usuario haya enviado en esos campos.
 *   - ADMIN: catalogoId es OPCIONAL. Si lo provee, se completa desde el catálogo
 *     igual que el USER. Si no, usa los datos enviados libremente en este DTO.
 */
@Data
public class SkinRequest {

    /**
     * ID del item del catálogo sobre el que se publica.
     * - USER: obligatorio. Si es null, el endpoint devuelve 400.
     * - ADMIN: opcional.
     */
    private Long catalogoId;

    // Nombre de la skin (ignorado si catalogoId no es null)
    private String name;

    // Descripción (ignorada si catalogoId no es null)
    private String description;

    // Precio de venta del vendedor
    private Double price;

    // Descuento aplicado por el vendedor (0.0 a 1.0). Default 0.
    private Double discount;

    // Nombre del videojuego (ej: "CS2"). Ignorado si catalogoId no es null.
    private String game;

    // URL pública de imagen (legacy — la imagen actual se sube como multipart).
    private String imageUrl;

    // Cantidad disponible en stock
    private Integer stock;

    // ID de la categoría asociada
    private Long categoryId;

    // Rareza: GRIS, CELESTE, AZUL, VIOLETA, ROSA, ROJO
    private String rareza;

    // Exterior: RECIEN_FABRICADO, CASI_NUEVO, ALGO_DESGASTADO, BASTANTE_DESGASTADO, DEPLORABLE
    private String exterior;

    // Si tiene contador StatTrak
    private Boolean stattrak;
}
