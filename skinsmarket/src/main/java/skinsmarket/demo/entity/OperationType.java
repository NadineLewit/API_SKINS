package skinsmarket.demo.entity;

/**
 * Tipo de operación que representa una Order.
 *
 *   PURCHASE  — el USER compra skins del marketplace (flujo carrito + MP)
 *   SALE      — el USER vende skins al bot (deposita en su inventario)
 *   EXCHANGE  — el USER intercambia skins propias por skins del marketplace
 *   RETURN    — devolución automática: el bot devuelve skins al USER cuando
 *               se cancela una venta o intercambio después de que el USER
 *               ya entregó sus skins.
 *
 * Antes de esta refactorización, Order solo soportaba PURCHASE (era implícito).
 * Ahora se hace explícito para poder distinguir flujos en el controller,
 * el service y el mock del bot.
 */
public enum OperationType {
    PURCHASE,
    SALE,
    EXCHANGE,
    BALANCE_TOP_UP,
    RETURN
}
