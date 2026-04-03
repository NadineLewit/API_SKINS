package skinsmarket.demo.controller.skin;

import lombok.Data;

/**
 *
 * Se utiliza en el SkinController para recibir datos del cliente sin exponer
 * directamente la entidad Skin.
 */
@Data
public class SkinRequest {

    // Identificador de la skin (usado principalmente en actualizaciones)
    private Long id;

    // Nombre de la skin (ej: "Dragon Lore", "Hyper Beast")
    private String name;

    // Precio de la skin en la plataforma
    private Double price;

    // Descuento aplicado sobre el precio (valor entre 0.0 y 1.0, ej: 0.15 = 15% off)
    private Double discount;

    // Nombre del videojuego al que pertenece la skin (ej: "CS2", "Dota 2")
    private String game;

    // URL pública de la imagen de la skin (generada tras subir el archivo)
    private String imageUrl;

    // Cantidad disponible en stock para la venta
    private Integer stock;

    // ID de la categoría a la que pertenece esta skin (ej: 1 = "Rifle", 2 = "Pistola")
    private Long categoryId;
}