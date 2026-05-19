package skinsmarket.demo.entity;

/**
 * Estado del trade (operación con el bot de Steam) para una Order.
 *
 * Es independiente del paymentStatus (que sigue siendo PENDING_PAYMENT / PAID /
 * REJECTED / etc, todo lo de Mercado Pago).
 *
 * Diagrama de transiciones por tipo de operación:
 *
 * PURCHASE:
 *   WAITING_PAYMENT → WAITING_UNLOCK → PREPARING_TRADE → BOT_SENT → COMPLETED
 *                  ↘ CANCELLED       ↘ EXPIRED / FAILED
 *
 * SALE:
 *   WAITING_USER_TRADE → USER_TRADE_RECEIVED → COMPLETED
 *                      ↘ CANCELLED (usuario no envió)
 *                      ↘ RETURN_PENDING → RETURN_SENT → RETURNED (canceló después de enviar)
 *
 * EXCHANGE:
 *   WAITING_USER_TRADE → USER_TRADE_RECEIVED → WAITING_DIFFERENCE → PREPARING_TRADE
 *                     → BOT_SENT → COMPLETED
 *                     (en cada paso se puede ir a CANCELLED o RETURN_PENDING si ya entregó skins)
 *
 * RETURN:
 *   RETURN_PENDING → RETURN_SENT → RETURNED
 *                                ↘ RETURN_FAILED
 */
public enum TradeStatus {
    /** Esperando que MP confirme el pago (solo PURCHASE). */
    WAITING_PAYMENT,

    /** Compra pagada, pero la skin sigue con trade lock antes de poder enviarla. */
    WAITING_UNLOCK,

    /** Esperando que el USER mande sus skins al bot (SALE / EXCHANGE). */
    WAITING_USER_TRADE,

    /** El bot recibió las skins del USER (SALE / EXCHANGE). */
    USER_TRADE_RECEIVED,

    /** Esperando pago de diferencia en intercambio (solo EXCHANGE). */
    WAITING_DIFFERENCE,

    /** Pago y skins OK, el bot va a enviar la oferta. */
    PREPARING_TRADE,

    /** El bot envió la oferta al usuario. Esperando que el USER acepte en Steam. */
    BOT_SENT,

    /** Trade completado: USER recibió sus skins (en PURCHASE/EXCHANGE) o el bot las recibió (en SALE). */
    COMPLETED,

    /** Operación cancelada antes de que el USER entregara skins. */
    CANCELLED,

    /** El trade offer expiró en Steam (sin que el USER lo aceptara). */
    EXPIRED,

    /** Algo falló en el flujo (error del bot, error de red, etc). */
    FAILED,

    /** Hay que devolver skins al USER (cancelación tardía o intercambio fallido). */
    RETURN_PENDING,

    /** Bot envió la trade offer de devolución. */
    RETURN_SENT,

    /** Devolución completada — USER aceptó la oferta de devolución. */
    RETURNED,

    /** La devolución falló (el USER no aceptó la oferta o expiró). */
    RETURN_FAILED
}
