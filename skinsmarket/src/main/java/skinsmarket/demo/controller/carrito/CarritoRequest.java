package skinsmarket.demo.controller.carrito;

import lombok.Data;

/**
 * DTO para las solicitudes relacionadas con el carrito de compras.
 *
 * Sigue el mismo patrón que WishlistRequest del TPO aprobado,
 * adaptado al dominio de skins: en lugar de gameId, usamos skinId.
 */
@Data
public class CarritoRequest {

    // ID de la skin que se desea agregar o modificar en el carrito
    private Long skinId;

    // Cantidad de unidades de la skin a agregar o actualizar
    private Integer cantidad;
}