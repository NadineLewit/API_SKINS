package skinsmarket.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una operación de la plataforma:
 *   - Compra (PURCHASE) — flujo original con Mercado Pago
 *   - Venta (SALE) — usuario deposita skins al bot
 *   - Intercambio (EXCHANGE) — mezcla skins propias y del marketplace
 *   - Devolución (RETURN) — el bot devuelve skins al usuario
 *
 * CAMPOS NUEVOS (sobre la versión original que solo soportaba PURCHASE):
 *   - operationType: distingue compra/venta/intercambio/devolución
 *   - tradeStatus: estado de la oferta con el bot de Steam
 *   - botTradeOfferId: id del trade offer real (cuando el bot esté activo)
 *   - expectedAssetIds: assetIds que el bot espera (para validar la oferta entrante)
 *   - userSkinAssetIds: assetIds que el USER ya entregó (para devolución)
 *   - priceDifference: en intercambios, monto que falta pagar/devolver
 *   - relatedOrderId: vincula una devolución con su orden original
 *
 * Mantengo @Table(name="orders") para no romper la BD existente.
 */
@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private Double descuentoAplicado = 0.0;

    @Column(nullable = false)
    private Double totalFinal = 0.0;

    @Column(nullable = false)
    private String paymentStatus = "PENDING_PAYMENT";

    private String mercadopagoPreferenceId;
    private Long mercadopagoPaymentId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    // =========================================================================
    // CAMPOS NUEVOS PARA FLUJO BOT
    // =========================================================================

    /**
     * Tipo de operación. Para órdenes pre-existentes (sin este campo en BD),
     * default a PURCHASE para no romper retrocompatibilidad.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private OperationType operationType = OperationType.PURCHASE;

    /**
     * Estado actual del trade con el bot.
     * Default: WAITING_PAYMENT (corresponde al flujo de compra clásica).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trade_status", length = 30)
    private TradeStatus tradeStatus = TradeStatus.WAITING_PAYMENT;

    /**
     * ID del trade offer real en Steam.
     * Mientras el bot esté en modo mock, lo dejamos null o con prefijo "MOCK-".
     * Cuando el bot real esté activo (después del 10/06), guardará el offer ID real.
     */
    @Column(name = "bot_trade_offer_id", length = 100)
    private String botTradeOfferId;

    /**
     * AssetIDs que el bot espera recibir/enviar en este trade.
     * Formato JSON serializado, ej: "[\"12345\",\"67890\"]".
     *
     * En PURCHASE: assetIds que el bot va a ENVIAR al usuario.
     * En SALE: assetIds que el bot espera RECIBIR del usuario.
     * En EXCHANGE: ambos lados (ver expectedAssetIds + userSkinAssetIds).
     * En RETURN: assetIds que el bot va a DEVOLVER al usuario.
     */
    @Column(name = "expected_asset_ids", length = 2000)
    private String expectedAssetIds;

    /**
     * En SALE y EXCHANGE: assetIds que el USER se comprometió a enviar.
     * Cuando el bot recibe el trade y los assetIds coinciden, marca
     * USER_TRADE_RECEIVED. Si no coinciden, rechaza.
     *
     * Si después se cancela, estos son los assetIds a devolver.
     */
    @Column(name = "user_skin_asset_ids", length = 2000)
    private String userSkinAssetIds;

    /**
     * Diferencia de precio en un intercambio.
     *   > 0  el USER debe pagar (genera preferencia MP por este monto)
     *   < 0  la plataforma debe acreditar saldo al USER
     *   = 0  intercambio directo, no hay diferencia
     */
    @Column(name = "price_difference")
    private Double priceDifference;

    @Column(name = "saldo_acreditado", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean saldoAcreditado = false;

    /**
     * Cuando esta order es una RETURN, apunta al ID de la orden original
     * que se canceló. Permite trackear "esta devolución corresponde a la venta #123".
     */
    @Column(name = "related_order_id")
    private Long relatedOrderId;

    // -------------------------------------------------------------------------
    // Métodos auxiliares de gestión de detalles
    // -------------------------------------------------------------------------

    public void addOrderDetail(OrderDetail d) {
        orderDetails.add(d);
        d.setOrder(this);
    }

    public void removeOrderDetail(OrderDetail d) {
        orderDetails.remove(d);
        d.setOrder(null);
    }
}
