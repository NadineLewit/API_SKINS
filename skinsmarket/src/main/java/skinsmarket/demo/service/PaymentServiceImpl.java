package skinsmarket.demo.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentStatus;
import com.mercadopago.resources.preference.Preference;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import skinsmarket.demo.controller.order.OrderDetailResponse;
import skinsmarket.demo.controller.order.OrderResponse;
import skinsmarket.demo.controller.payment.BrickPaymentRequest;
import skinsmarket.demo.controller.payment.BrickPaymentResponse;
import skinsmarket.demo.controller.payment.BrickPreferenceResponse;
import skinsmarket.demo.controller.payment.MercadoPagoWebhookRequest;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.OrderDetail;
import skinsmarket.demo.repository.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Value("${mercadopago.backend-url}")
    private String backendUrl;

    @Value("${mercadopago.currency-id}")
    private String currencyId;

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BrickPreferenceResponse createBrickPreferenceFromCarrito(String email, String codigoCupon) throws Exception {
        OrderResponse orderResponse = orderService.createOrderFromCarrito(email, codigoCupon);
        return createPreferenceForOrder(email, orderResponse.getId());
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BrickPreferenceResponse createBrickPreferenceForExistingOrder(String email, Long orderId) throws Exception {
        return createPreferenceForOrder(email, orderId);
    }

    private BrickPreferenceResponse createPreferenceForOrder(String email, Long orderId) throws Exception {
        ensureConfigured();
        ensurePublicKeyConfigured();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("La orden no pertenece al usuario autenticado");
        }
        validatePaymentAmount(order);

        MercadoPagoConfig.setAccessToken(accessToken);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id("ORDER-" + order.getId())
                .title("Orden #" + order.getId() + " - Skins Market")
                .description(buildDescription(order))
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(order.getTotalFinal()).setScale(2, RoundingMode.HALF_UP))
                .currencyId(currencyId)
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(backendUrl + "/payments/return/success")
                .failure(backendUrl + "/payments/return/failure")
                .pending(backendUrl + "/payments/return/pending")
                .build();

        PreferencePayerRequest payer = PreferencePayerRequest.builder()
                .email(order.getUser().getEmail())
                .build();

        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .payer(payer)
                .externalReference(order.getId().toString())
                .backUrls(backUrls)
                .purpose("wallet_purchase")
                .notificationUrl(backendUrl + "/payments/webhook")
                .build();

        Preference preference = new PreferenceClient().create(preferenceRequest);

        order.setPaymentStatus("PENDING_PAYMENT");
        order.setMercadopagoPreferenceId(preference.getId());
        orderRepository.save(order);

        return new BrickPreferenceResponse(
                mapToOrderResponse(order),
                preference.getId(),
                publicKey,
                order.getPaymentStatus()
        );
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BrickPaymentResponse processBrickPayment(
            String email,
            Long orderId,
            BrickPaymentRequest request,
            String idempotencyKey) throws Exception {
        ensureConfigured();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("La orden no pertenece al usuario autenticado");
        }
        if (request == null) {
            throw new RuntimeException("Faltan los datos del pago enviados por Payment Brick");
        }
        if (request.getPaymentMethodId() == null || request.getPaymentMethodId().isBlank()) {
            throw new RuntimeException("Falta payment_method_id/paymentMethodId");
        }
        validatePaymentAmount(order);

        MercadoPagoConfig.setAccessToken(accessToken);

        PaymentCreateRequest.PaymentCreateRequestBuilder paymentBuilder = PaymentCreateRequest.builder()
                .transactionAmount(BigDecimal.valueOf(order.getTotalFinal()).setScale(2, RoundingMode.HALF_UP))
                .token(blankToNull(request.getToken()))
                .description(firstNonBlank(request.getDescription(), buildDescription(order)))
                .installments(request.getInstallments() != null ? request.getInstallments() : 1)
                .paymentMethodId(request.getPaymentMethodId())
                .issuerId(blankToNull(request.getIssuerId()))
                .externalReference(order.getId().toString())
                .payer(buildPayer(order, request));

        if (isPublicHttpsBackendUrl()) {
            paymentBuilder.notificationUrl(backendUrl + "/payments/webhook");
        }

        PaymentCreateRequest paymentCreateRequest = paymentBuilder.build();

        Payment payment = new PaymentClient().create(paymentCreateRequest, buildRequestOptions(idempotencyKey));
        updateOrderWithPayment(order, payment);

        return new BrickPaymentResponse(
                mapToOrderResponse(order),
                payment.getId(),
                payment.getStatus(),
                payment.getStatusDetail(),
                payment.getPaymentMethodId(),
                payment.getPaymentTypeId(),
                payment.getTransactionDetails() != null
                        ? payment.getTransactionDetails().getExternalResourceUrl()
                        : null
        );
    }

    @Override
    @Transactional
    public void processWebhook(String type, String topic, String id, MercadoPagoWebhookRequest body) {
        String notificationType = firstNonBlank(firstNonBlank(type, topic), body != null ? body.getType() : null);
        String paymentId = firstNonBlank(id, body != null && body.getData() != null ? body.getData().getId() : null);

        if (!"payment".equals(notificationType) || paymentId == null) {
            return;
        }

        try {
            ensureConfigured();
            MercadoPagoConfig.setAccessToken(accessToken);

            Payment payment = new PaymentClient().get(Long.valueOf(paymentId));
            Long orderId = Long.valueOf(payment.getExternalReference());

            orderRepository.findById(orderId).ifPresent(order -> {
                updateOrderWithPayment(order, payment);
            });
        } catch (Exception e) {
            throw new RuntimeException("No se pudo procesar la notificacion de Mercado Pago", e);
        }
    }

    private void ensureConfigured() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Falta configurar MERCADOPAGO_ACCESS_TOKEN");
        }
    }

    private void ensurePublicKeyConfigured() {
        if (publicKey == null || publicKey.isBlank()) {
            throw new RuntimeException("Falta configurar MERCADOPAGO_PUBLIC_KEY");
        }
    }

    private void validatePaymentAmount(Order order) {
        if (order.getTotalFinal() == null || order.getTotalFinal() <= 0) {
            throw new RuntimeException(
                    "La orden " + order.getId() + " tiene totalFinal inválido para pagar: "
                            + order.getTotalFinal() + ". Usá el order_id devuelto por la preferencia más reciente "
                            + "y verificá que la publicación tenga precio mayor a 0."
            );
        }
    }

    private PaymentPayerRequest buildPayer(Order order, BrickPaymentRequest request) {
        BrickPaymentRequest.Payer payer = request.getPayer();
        PaymentPayerRequest.PaymentPayerRequestBuilder builder = PaymentPayerRequest.builder()
                .email(payer != null && payer.getEmail() != null && !payer.getEmail().isBlank()
                        ? payer.getEmail()
                        : order.getUser().getEmail());

        if (payer != null) {
            builder.firstName(blankToNull(payer.getFirstName()));
            builder.lastName(blankToNull(payer.getLastName()));

            if (payer.getIdentification() != null
                    && payer.getIdentification().getType() != null
                    && payer.getIdentification().getNumber() != null) {
                builder.identification(IdentificationRequest.builder()
                        .type(payer.getIdentification().getType())
                        .number(payer.getIdentification().getNumber())
                        .build());
            }
        }

        return builder.build();
    }

    private MPRequestOptions buildRequestOptions(String idempotencyKey) {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("x-idempotency-key",
                idempotencyKey != null && !idempotencyKey.isBlank()
                        ? idempotencyKey
                        : UUID.randomUUID().toString());

        return MPRequestOptions.builder()
                .customHeaders(customHeaders)
                .build();
    }

    private void updateOrderWithPayment(Order order, Payment payment) {
        order.setMercadopagoPaymentId(payment.getId());
        order.setPaymentStatus(mapPaymentStatus(payment.getStatus()));
        orderRepository.save(order);
    }

    private boolean isPublicHttpsBackendUrl() {
        return backendUrl != null && backendUrl.startsWith("https://");
    }

    private String mapPaymentStatus(String status) {
        if (PaymentStatus.APPROVED.equals(status)) return "PAID";
        if (PaymentStatus.REJECTED.equals(status)) return "REJECTED";
        if (PaymentStatus.CANCELLED.equals(status)) return "CANCELLED";
        if (PaymentStatus.REFUNDED.equals(status)) return "REFUNDED";
        if (PaymentStatus.CHARGED_BACK.equals(status)) return "CHARGED_BACK";
        if (PaymentStatus.IN_PROCESS.equals(status)) return "IN_PROCESS";
        if (PaymentStatus.PENDING.equals(status)) return "PENDING_PAYMENT";
        return status != null ? status.toUpperCase() : "UNKNOWN";
    }

    private String buildDescription(Order order) {
        return order.getOrderDetails().stream()
                .map(detail -> detail.getQuantity() + "x " + detail.getSkin().getName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Compra de skins");
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setEmail(order.getUser().getEmail());
        response.setDate(order.getDate());
        response.setTotalPrice(order.getTotalPrice());
        response.setDescuentoAplicado(order.getDescuentoAplicado());
        response.setTotalFinal(order.getTotalFinal());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setMercadopagoPreferenceId(order.getMercadopagoPreferenceId());
        response.setMercadopagoPaymentId(order.getMercadopagoPaymentId());
        response.setOrderDetailResponses(order.getOrderDetails().stream()
                .map(this::mapDetail)
                .toList());
        return response;
    }

    private OrderDetailResponse mapDetail(OrderDetail detail) {
        OrderDetailResponse response = new OrderDetailResponse();
        response.setSkinId(detail.getSkin() != null ? detail.getSkin().getId() : null);
        response.setSkinName(detail.getSkin() != null ? detail.getSkin().getName() : null);
        response.setQuantity(detail.getQuantity());
        response.setUnitPrice(detail.getUnitPrice());
        return response;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }

    private String blankToNull(String value) {
        return value != null && !value.isBlank() ? value : null;
    }
}
