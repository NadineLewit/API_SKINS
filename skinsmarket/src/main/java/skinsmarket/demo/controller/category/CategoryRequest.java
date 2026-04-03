package skinsmarket.demo.controller.category;

import lombok.Data;

/**
 * DTO para las solicitudes de creación de categorías.
 *
 * Transporta únicamente el nombre de la categoría desde el cliente.
 */
@Data
public class CategoryRequest {

    // ID interno (no siempre es necesario en la request, pero se mantiene por consistencia)
    private int id;

    // Nombre de la categoría (ej: "Rifle", "Pistola", "Cuchillo", "Guante")
    private String name;
}