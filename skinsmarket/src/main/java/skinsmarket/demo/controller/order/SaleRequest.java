package skinsmarket.demo.controller.order;

import lombok.Data;

import java.util.List;

/**
 * Request para crear una orden de VENTA: el USER deposita skins en el bot
 * a cambio de saldo interno / pago.
 *
 * El usuario elige items de SU inventario (inventario_items.id) que ya está
 * sincronizado desde Steam. El backend resuelve los assetIds reales a partir
 * de esos IDs internos.
 */
@Data
public class SaleRequest {

    /** IDs de InventarioItem (no de Skin, no de assetId — el id interno de la tabla). */
    private List<Long> inventarioItemIds;

    /**
     * Precio total que el USER pide por todas las skins.
     * El admin/sistema decide después si lo acepta o no (en este TPO lo aceptamos siempre).
     */
    private Double precioOfrecido;
}
