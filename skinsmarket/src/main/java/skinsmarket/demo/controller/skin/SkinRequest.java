package skinsmarket.demo.controller.skin;

import lombok.Data;

/**
 * DTO para las solicitudes de creación y edición de Skins.
 *
 * NO incluye id: en creación JPA lo genera automáticamente (auto_increment).
 * En edición el id va en el path (/skins/admin/{id} o /skins/{id}), no en el body.
 * Enviar un id en el body no tiene ningún efecto y genera confusión.
 */
@Data
public class SkinRequest {

    // Nombre de la skin (ej: "Dragon Lore", "Hyper Beast")
    private String name;

    // Descripción de la skin
    private String description;

    // Precio de la skin
    private Double price;

    // Descuento aplicado (0.0 a 1.0, ej: 0.15 = 15% off). Default 0.
    private Double discount;

    // Nombre del videojuego (ej: "CS2", "Dota 2")
    private String game;

    // URL pública de la imagen
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