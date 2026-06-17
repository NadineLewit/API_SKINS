package skinsmarket.demo.controller.inventario;

import lombok.Data;

/**
 * DTO para publicar un item del inventario a la venta.
 *
 * El usuario solo decide PRECIO y DESCUENTO. El resto (nombre, descripción,
 * imagen, rareza, etc.) se hereda automáticamente del SkinCatalogo asociado
 * al item del inventario.
 *
 * Stock siempre será 1 porque un item del inventario de Steam es un asset
 * físico único (no se duplica).
 */
@Data
public class PublicarDesdeInventarioRequest {

    /** Precio de venta del usuario (obligatorio, mayor a 0). */
    private Double price;

    /** Descuento opcional (0.0 a 1.0). Default 0. */
    private Double discount;

    /** Si esta publicación acepta intercambio. Default true. */
    private Boolean intercambiable;

    /** Si esta publicación se puede comprar por carrito/Mercado Pago. Default true. */
    private Boolean vendible;
}
