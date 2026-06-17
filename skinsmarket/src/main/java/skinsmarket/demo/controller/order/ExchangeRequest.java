package skinsmarket.demo.controller.order;

import lombok.Data;

import java.util.List;

/**
 * Request para crear una orden de INTERCAMBIO.
 *
 * El usuario entrega N skins de su inventario y recibe M skins del marketplace.
 * El backend calcula la diferencia de precio:
 *   - valor_marketplace = sum(skin.finalPrice del marketplace)
 *   - valor_usuario     = sum(precio promedio del marketplace para esas skins)
 *
 *   diferencia = valor_marketplace - valor_usuario
 *
 * Si diferencia > 0 → USER paga la diferencia con MP
 * Si diferencia < 0 → plataforma le debe saldo al USER
 * Si diferencia = 0 → intercambio directo
 */
@Data
public class ExchangeRequest {

    /** IDs de InventarioItem del USER que entrega. */
    private List<Long> inventarioItemIds;

    /** IDs de Skin del marketplace que el USER quiere recibir. */
    private List<Long> skinIds;
}
