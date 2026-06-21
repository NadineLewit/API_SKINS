package skinsmarket.demo.service;

import skinsmarket.demo.controller.order.ExchangeRequest;
import skinsmarket.demo.controller.order.ExchangeQuoteResponse;
import skinsmarket.demo.controller.order.OperationStatusResponse;
import skinsmarket.demo.controller.order.SaleRequest;
import skinsmarket.demo.entity.InventarioItem;

import java.util.List;

/**
 * Service para operaciones que requieren al bot de Steam:
 *   - Venta (USER → BOT)
 *   - Intercambio (USER ↔ BOT)
 *   - Cancelación (puede generar devolución automática)
 *
 * La compra clásica (PURCHASE) sigue manejándose por OrderService +
 * PaymentService, pero esos también empiezan a setear operationType y
 * tradeStatus desde acá (ver OrderServiceImpl modificado).
 */
public interface TradeOperationService {

    /** Crea una orden de venta — el USER entrega skins al bot a cambio de pago. */
    OperationStatusResponse createSale(String email, SaleRequest request);

    /** Crea una orden de intercambio. */
    OperationStatusResponse createExchange(String email, ExchangeRequest request);

    /** Cotiza un intercambio sin reservar skins ni crear orden. */
    ExchangeQuoteResponse quoteExchange(String email, ExchangeRequest request);

    /** Estima el valor de una skin del inventario con la misma regla del intercambio. */
    double estimateInventoryItemPrice(InventarioItem item);

    /** Cancela una operación. Si el USER ya entregó skins, genera una RETURN. */
    OperationStatusResponse cancelOperation(String email, Long orderId);

    /**
     * El bot llama esto (vía endpoint protegido) cuando recibió las skins del USER
     * y los assetIds coinciden con los esperados. Pasa el trade a USER_TRADE_RECEIVED.
     */
    OperationStatusResponse markUserTradeReceived(Long orderId);

    /** Consulta estado completo de una operación. Valida pertenencia al USER. */
    OperationStatusResponse getStatus(String email, Long orderId);

    /** Lista todas las operaciones del USER. */
    List<OperationStatusResponse> listMyOperations(String email);
}
