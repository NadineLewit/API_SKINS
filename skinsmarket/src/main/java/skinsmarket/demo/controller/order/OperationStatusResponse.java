package skinsmarket.demo.controller.order;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response completo de una operación (compra/venta/intercambio/devolución).
 * Sirve para el endpoint GET /operations/{id}/status.
 */
@Data
public class OperationStatusResponse {

    private Long id;
    private String operationType;
    private String tradeStatus;
    private String paymentStatus;
    private String email;
    private LocalDateTime date;

    private Double totalPrice;
    private Double totalFinal;
    private Double descuentoAplicado;
    private Double priceDifference;
    private Double saldoARecibir;
    private Boolean saldoAcreditado;

    private String botTradeOfferId;
    private String expectedAssetIds;
    private String userSkinAssetIds;

    private String mercadopagoPreferenceId;
    private Long mercadopagoPaymentId;

    private Long relatedOrderId;

    private List<OrderDetailResponse> orderDetailResponses;

    /** Mensaje en español describiendo el estado actual (para mostrar al user). */
    private String mensajeEstado;
}
