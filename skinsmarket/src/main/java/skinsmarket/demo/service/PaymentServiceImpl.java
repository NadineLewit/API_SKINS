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
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import skinsmarket.demo.controller.order.OrderDetailResponse;
import skinsmarket.demo.controller.order.OrderResponse;
import skinsmarket.demo.controller.payment.BalanceTopUpPaymentRequest;
import skinsmarket.demo.controller.payment.BrickConfigResponse;
import skinsmarket.demo.controller.payment.BrickPaymentRequest;
import skinsmarket.demo.controller.payment.BrickPaymentResponse;
import skinsmarket.demo.controller.payment.BrickPreferenceResponse;
import skinsmarket.demo.controller.payment.MercadoPagoWebhookRequest;
import skinsmarket.demo.controller.payment.TestCardPaymentRequest;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.OperationType;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.OrderDetail;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.TradeStatus;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.InventarioItemRepository;
import skinsmarket.demo.repository.OrderRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * PaymentServiceImpl con integración al archivo orders.json del bot.
 *
 * CAMBIO sobre la versión anterior:
 *   - Cuando una orden pasa a paymentStatus=PAID, ADEMÁS de actualizar la BD,
 *     se escribe una entrada en orders.json para que el bot prepare la entrega
 *     de las skins compradas. El MockTradeScheduler también detecta el PAID y
 *     avanza el tradeStatus a PREPARING_TRADE → BOT_SENT → COMPLETED.
 *
 * El resto de la lógica MP (preferencias, brick, webhook) sigue igual.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> MERCADO_PAGO_TEST_CARDS = Set.of(
            "4509953566233704",
            "4002768694395619",
            "5031755734530604",
            "5287338310253304",
            "371180303257522"
    );

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    /** ✨ NUEVO: para escribir entradas en orders.json al confirmar pago. */
    @Autowired
    private BotTradeOrdersFileService botFileService;

    @Autowired
    private InventarioItemRepository inventarioItemRepository;

    @Autowired
    private InventarioItemCreadorHelper inventarioItemCreadorHelper;

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventoService eventoService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Value("${mercadopago.sandbox-access-token:}")
    private String sandboxAccessToken;

    @Value("${mercadopago.sandbox-public-key:}")
    private String sandboxPublicKey;

    @Value("${mercadopago.backend-url}")
    private String backendUrl;

    @Value("${mercadopago.currency-id}")
    private String currencyId;

    @Value("${application.balance.usd-to-ars:1451.02}")
    private double usdToArs;

    @Value("${mock.enabled:true}")
    private boolean mockEnabled;

    @PostConstruct
    void useSharedSandboxCredentialsWhenConfiguredValuesArePlaceholders() {
        if (isPlaceholderCredential(accessToken) && sandboxAccessToken != null && !sandboxAccessToken.isBlank()) {
            accessToken = sandboxAccessToken;
        }
        if (isPlaceholderCredential(publicKey) && sandboxPublicKey != null && !sandboxPublicKey.isBlank()) {
            publicKey = sandboxPublicKey;
        }
    }

    @Override
    public BrickConfigResponse getBrickConfig() {
        ensurePublicKeyConfigured();
        return new BrickConfigResponse(publicKey);
    }

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

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BrickPreferenceResponse createBalanceTopUpPreference(
            String email,
            BalanceTopUpPaymentRequest request) throws Exception {
        Order order = createBalanceTopUpOrder(email, request);
        return createPreferenceForOrder(email, order.getId());
    }

    private BrickPreferenceResponse createPreferenceForOrder(String email, Long orderId) throws Exception {
        ensureConfigured();
        ensurePublicKeyConfigured();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("La orden no pertenece al usuario autenticado");
        }
        if ("PAID".equals(order.getPaymentStatus())) {
            throw new RuntimeException("La orden ya fue pagada");
        }
        validatePayableOrder(order);
        validarPublicacionesDisponiblesParaPago(order);

        if (mockEnabled && hasPlaceholderTestCredentials()) {
            String localPreferenceId = "LOCAL-" + order.getId();
            order.setPaymentStatus("PENDING_PAYMENT");
            order.setMercadopagoPreferenceId(localPreferenceId);
            orderRepository.save(order);
            return new BrickPreferenceResponse(
                    mapToOrderResponse(order),
                    localPreferenceId,
                    publicKey,
                    order.getPaymentStatus(),
                    null,
                    null,
                    null,
                    "local"
            );
        }

        MercadoPagoConfig.setAccessToken(accessToken);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id("ORDER-" + order.getId())
                .title("Orden #" + order.getId() + " - Skins Market")
                .description(buildDescription(order))
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(mercadoPagoAmount(order)).setScale(2, RoundingMode.HALF_UP))
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

        PreferenceRequest.PreferenceRequestBuilder preferenceBuilder = PreferenceRequest.builder()
                .items(List.of(item))
                .payer(payer)
                .externalReference(order.getId().toString())
                .backUrls(backUrls)
                .purpose("wallet_purchase");

        if (isPublicHttpsBackendUrl()) {
            preferenceBuilder.notificationUrl(backendUrl + "/payments/webhook");
        }

        PreferenceRequest preferenceRequest = preferenceBuilder.build();

        Preference preference = new PreferenceClient().create(preferenceRequest);

        order.setPaymentStatus("PENDING_PAYMENT");
        order.setMercadopagoPreferenceId(preference.getId());
        orderRepository.save(order);

        return new BrickPreferenceResponse(
                mapToOrderResponse(order),
                preference.getId(),
                publicKey,
                order.getPaymentStatus(),
                preference.getInitPoint(),
                preference.getSandboxInitPoint(),
                checkoutUrlFor(preference),
                effectiveCheckoutMode()
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

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("La orden no pertenece al usuario autenticado");
        }
        if ("PAID".equals(order.getPaymentStatus())) {
            return approvedExistingPaymentResponse(order);
        }
        if (request == null) {
            throw new RuntimeException("Faltan los datos del pago enviados por Payment Brick");
        }
        if (request.getPaymentMethodId() == null || request.getPaymentMethodId().isBlank()) {
            throw new RuntimeException("Falta payment_method_id/paymentMethodId");
        }
        validatePayableOrder(order);
        validarPublicacionesDisponiblesParaPago(order);

        MercadoPagoConfig.setAccessToken(accessToken);

        PaymentCreateRequest.PaymentCreateRequestBuilder paymentBuilder = PaymentCreateRequest.builder()
                .transactionAmount(BigDecimal.valueOf(mercadoPagoAmount(order)).setScale(2, RoundingMode.HALF_UP))
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
    @Transactional(rollbackOn = Exception.class)
    public BrickPaymentResponse processTestCardPayment(
            String email,
            Long orderId,
            TestCardPaymentRequest request,
            String idempotencyKey) throws Exception {
        ensureConfigured();
        ensurePublicKeyConfigured();
        ensureTestCredentials();

        if (request == null) {
            throw new RuntimeException("Faltan los datos de la tarjeta de prueba");
        }

        String cardNumber = normalizeCardNumber(request.getCardNumber());
        if (!MERCADO_PAGO_TEST_CARDS.contains(cardNumber)) {
            throw new RuntimeException("Usa una tarjeta de prueba oficial de Mercado Pago");
        }
        validateTestCardData(request, email);

        if (mockEnabled && hasPlaceholderTestCredentials()) {
            return processLocalTestCardPayment(email, orderId, request, cardNumber);
        }

        String token = createTestCardToken(request, cardNumber);
        BrickPaymentRequest brickRequest = new BrickPaymentRequest();
        brickRequest.setToken(token);
        brickRequest.setPaymentMethodId(firstNonBlank(
                request.getPaymentMethodId(),
                inferPaymentMethodId(cardNumber)
        ));
        brickRequest.setInstallments(request.getInstallments() != null ? request.getInstallments() : 1);

        BrickPaymentRequest.Payer payer = new BrickPaymentRequest.Payer();
        payer.setEmail(firstNonBlank(request.getEmail(), email));
        payer.setFirstName(blankToNull(request.getCardholderName()));

        BrickPaymentRequest.Identification identification = new BrickPaymentRequest.Identification();
        identification.setType(firstNonBlank(request.getDocumentType(), "DNI"));
        identification.setNumber(firstNonBlank(request.getDocumentNumber(), "12345678"));
        payer.setIdentification(identification);

        brickRequest.setPayer(payer);
        return processBrickPayment(email, orderId, brickRequest, idempotencyKey);
    }

    @Override
    @Transactional
    public BrickPaymentResponse processBalancePayment(String email, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("La orden no pertenece al usuario autenticado");
        }
        if ("PAID".equals(order.getPaymentStatus())) {
            return approvedBalanceResponse(order);
        }
        if (order.getMercadopagoPreferenceId() != null
                && "PENDING_PAYMENT".equals(order.getPaymentStatus())) {
            throw new RuntimeException("Ya iniciaste el pago con Mercado Pago para esta orden");
        }

        validatePayableOrder(order);
        validarPublicacionesDisponiblesParaPago(order);

        double total = roundMoney(paymentAmount(order));
        double balance = roundMoney(user.getSaldo() != null ? user.getSaldo() : 0.0);
        if (balance < total) {
            throw new RuntimeException(String.format(
                    Locale.forLanguageTag("es-AR"),
                    "Saldo insuficiente. Te faltan $%.2f USD.", roundMoney(total - balance)));
        }

        user.setSaldo(roundMoney(balance - total));
        userRepository.save(user);
        updateOrderPaymentState(order, null, "PAID");
        return approvedBalanceResponse(order);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BrickPaymentResponse processBalanceTopUpTestCard(
            String email,
            BalanceTopUpPaymentRequest request,
            String idempotencyKey) throws Exception {
        Order order = createBalanceTopUpOrder(email, request);

        if (request.getToken() != null && !request.getToken().isBlank()) {
            BrickPaymentRequest brickRequest = new BrickPaymentRequest();
            brickRequest.setToken(request.getToken());
            brickRequest.setPaymentMethodId(request.getPaymentMethodId());
            brickRequest.setIssuerId(request.getIssuerId());
            brickRequest.setInstallments(request.getInstallments());
            brickRequest.setTransactionAmount(request.getTransactionAmount() != null
                    ? BigDecimal.valueOf(request.getTransactionAmount())
                    : BigDecimal.valueOf(request.getAmountArs()));
            brickRequest.setDescription("Recarga de saldo de intercambio");
            brickRequest.setPayer(request.getPayer());
            return processBrickPayment(email, order.getId(), brickRequest, idempotencyKey);
        }

        return processTestCardPayment(email, order.getId(), request, idempotencyKey);
    }

    private Order createBalanceTopUpOrder(String email, BalanceTopUpPaymentRequest request) {
        if (request == null || request.getAmountArs() == null) {
            throw new RuntimeException("El importe de la recarga es obligatorio");
        }

        double amountArs = request.getAmountArs();
        double maxAmountArs = usdToArs * 3000.0;
        if (!Double.isFinite(amountArs) || amountArs < 1500.0 || amountArs > maxAmountArs) {
            throw new RuntimeException(
                    "El importe debe estar entre ARS 1.500 y ARS " + Math.round(maxAmountArs));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        double amountUsd = BigDecimal.valueOf(amountArs / usdToArs)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        Order order = new Order();
        order.setUser(user);
        order.setDate(LocalDateTime.now());
        order.setTotalPrice(amountArs);
        order.setDescuentoAplicado(0.0);
        order.setTotalFinal(amountArs);
        order.setPaymentStatus("PENDING_PAYMENT");
        order.setOperationType(OperationType.BALANCE_TOP_UP);
        order.setTradeStatus(TradeStatus.WAITING_PAYMENT);
        order.setPriceDifference(amountUsd);
        order.setSaldoAcreditado(false);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BrickPaymentResponse syncPaymentStatus(String email, Long orderId) throws Exception {
        ensureConfigured();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("La orden no pertenece al usuario autenticado");
        }

        if (mockEnabled && hasPlaceholderTestCredentials()) {
            return new BrickPaymentResponse(
                    mapToOrderResponse(order),
                    order.getMercadopagoPaymentId(),
                    paymentStatusToMercadoPagoStatus(order.getPaymentStatus()),
                    null,
                    null,
                    null,
                    null
            );
        }

        MercadoPagoConfig.setAccessToken(accessToken);

        MpPaymentSnapshot payment = null;
        if (order.getMercadopagoPaymentId() != null) {
            payment = findPaymentById(order.getMercadopagoPaymentId());
        } else {
            payment = findPaymentByExternalReference(order.getId().toString());
        }

        if (payment != null) {
            updateOrderWithPaymentSnapshot(order, payment);
            return mapToBrickPaymentResponse(order, payment);
        }

        return new BrickPaymentResponse(
                mapToOrderResponse(order),
                null,
                paymentStatusToMercadoPagoStatus(order.getPaymentStatus()),
                null,
                null,
                null,
                null
        );
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public BrickPaymentResponse syncPendingPayments(String email) throws Exception {
        ensureConfigured();

        if (mockEnabled && hasPlaceholderTestCredentials()) {
            return new BrickPaymentResponse(
                    null,
                    null,
                    PaymentStatus.PENDING,
                    null,
                    null,
                    null,
                    null
            );
        }

        MercadoPagoConfig.setAccessToken(accessToken);

        List<String> pendingStatuses = List.of("PENDING_PAYMENT", "IN_PROCESS");
        List<Order> pendingOrders = orderRepository
                .findByUserEmailAndPaymentStatusInOrderByDateDesc(email, pendingStatuses);

        BrickPaymentResponse latestResponse = null;
        for (Order pendingOrder : pendingOrders) {
            MpPaymentSnapshot payment = findPaymentByExternalReference(pendingOrder.getId().toString());
            if (payment == null) continue;

            updateOrderWithPaymentSnapshot(pendingOrder, payment);
            BrickPaymentResponse response = mapToBrickPaymentResponse(pendingOrder, payment);
            latestResponse = response;

            if (PaymentStatus.APPROVED.equals(payment.status())) {
                return response;
            }
        }

        if (latestResponse != null) {
            return latestResponse;
        }

        return new BrickPaymentResponse(
                null,
                null,
                PaymentStatus.PENDING,
                null,
                null,
                null,
                null
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

            MpPaymentSnapshot payment = findPaymentById(Long.valueOf(paymentId));
            if (payment == null || payment.externalReference() == null) {
                return;
            }

            Long orderId = Long.valueOf(payment.externalReference());
            orderRepository.findById(orderId).ifPresent(order ->
                    updateOrderWithPaymentSnapshot(order, payment));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo procesar la notificacion de Mercado Pago", e);
        }
    }

    /**
     * Cuando el payment se actualiza, si pasa a PAID y es una compra,
     * entrega ahora si la skin ya está libre o crea una reserva interna si
     * todavía está bloqueada.
     */
    private void updateOrderWithPayment(Order order, Payment payment) {
        updateOrderPaymentState(order, payment.getId(), mapPaymentStatus(payment.getStatus()));
    }

    private void updateOrderWithPaymentSnapshot(Order order, MpPaymentSnapshot payment) {
        updateOrderPaymentState(order, payment.id(), mapPaymentStatus(payment.status()));
    }

    private void updateOrderPaymentState(Order order, Long paymentId, String newStatus) {
        String previousStatus = order.getPaymentStatus();
        order.setMercadopagoPaymentId(paymentId);
        order.setPaymentStatus(newStatus);
        orderRepository.save(order);

        if (esPagoFallidoOCerrado(newStatus) && !esPagoFallidoOCerrado(previousStatus)) {
            liberarPublicacionesReservadas(order);
        }

        if ("PAID".equals(newStatus)
                && order.getOperationType() == OperationType.EXCHANGE
                && order.getTradeStatus() == TradeStatus.WAITING_DIFFERENCE) {
            order.setTradeStatus(TradeStatus.PREPARING_TRADE);
            orderRepository.save(order);
            botFileService.updateStatus(order.getId(), order.getTradeStatus().name());
        }

        if ("PAID".equals(newStatus)
                && order.getOperationType() == OperationType.BALANCE_TOP_UP
                && !Boolean.TRUE.equals(order.getSaldoAcreditado())) {
            acreditarRecargaSaldo(order);
        }

        // Si la compra fue pagada con éxito y es PURCHASE, se entrega ahora o
        // queda reservada si alguna skin todavía está bloqueada.
        if ("PAID".equals(newStatus) &&
                !"PAID".equals(previousStatus) &&
                order.getOperationType() == OperationType.PURCHASE) {
            reservarPublicacionesPagadas(order);
            registrarVentasSiCorresponde(order, previousStatus);
            crearItemsPendientesPorCompra(order);
            if (tieneSkinsBloqueadas(order)) {
                order.setTradeStatus(TradeStatus.WAITING_UNLOCK);
                orderRepository.save(order);
            } else {
                crearBotOrderParaCompra(order);
            }
        }
    }

    private MpPaymentSnapshot findPaymentById(Long paymentId) {
        if (paymentId == null) return null;
        String url = "https://api.mercadopago.com/v1/payments/" + paymentId;
        return snapshotFromMap(mercadoPagoGet(url));
    }

    private MpPaymentSnapshot findPaymentByExternalReference(String externalReference) {
        String url = UriComponentsBuilder
                .fromUriString("https://api.mercadopago.com/v1/payments/search")
                .queryParam("external_reference", externalReference)
                .queryParam("sort", "date_created")
                .queryParam("criteria", "desc")
                .queryParam("limit", 10)
                .encode()
                .toUriString();

        Map<String, Object> body = mercadoPagoGet(url);
        Object results = body != null ? body.get("results") : null;
        if (!(results instanceof List<?> list) || list.isEmpty()) {
            return null;
        }

        MpPaymentSnapshot first = null;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) continue;
            MpPaymentSnapshot snapshot = snapshotFromMap(raw);
            if (snapshot == null) continue;
            if (first == null) first = snapshot;
            if (PaymentStatus.APPROVED.equals(snapshot.status())) return snapshot;
        }
        return first;
    }

    private Map<String, Object> mercadoPagoGet(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            return body;
        } catch (Exception e) {
            return null;
        }
    }

    private MpPaymentSnapshot snapshotFromMap(Map<?, ?> raw) {
        if (raw == null || raw.isEmpty()) return null;
        Long id = longValue(raw.get("id"));
        String status = textValue(raw.get("status"));
        if (status == null || status.isBlank()) return null;

        String externalResourceUrl = null;
        Object transactionDetails = raw.get("transaction_details");
        if (transactionDetails instanceof Map<?, ?> details) {
            externalResourceUrl = textValue(details.get("external_resource_url"));
        }

        return new MpPaymentSnapshot(
                id,
                textValue(raw.get("external_reference")),
                status,
                textValue(raw.get("status_detail")),
                textValue(raw.get("payment_method_id")),
                textValue(raw.get("payment_type_id")),
                externalResourceUrl
        );
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String textValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BrickPaymentResponse mapToBrickPaymentResponse(Order order, Payment payment) {
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

    private BrickPaymentResponse mapToBrickPaymentResponse(Order order, MpPaymentSnapshot payment) {
        return new BrickPaymentResponse(
                mapToOrderResponse(order),
                payment.id(),
                payment.status(),
                payment.statusDetail(),
                payment.paymentMethodId(),
                payment.paymentTypeId(),
                payment.externalResourceUrl()
        );
    }

    private record MpPaymentSnapshot(
            Long id,
            String externalReference,
            String status,
            String statusDetail,
            String paymentMethodId,
            String paymentTypeId,
            String externalResourceUrl) {
    }

    private void validarPublicacionesDisponiblesParaPago(Order order) {
        if (order.getOperationType() != OperationType.PURCHASE) return;

        for (OrderDetail d : detallesOrdenados(order)) {
            Skin skin = skinRepository.findByIdForUpdate(d.getSkin().getId())
                    .orElseThrow(() -> new RuntimeException("La skin de la orden ya no existe"));
            validarPublicacionDisponible(skin);
        }
    }

    private void reservarPublicacionesPagadas(Order order) {
        if (order.getOperationType() != OperationType.PURCHASE) return;

        for (OrderDetail d : detallesOrdenados(order)) {
            Skin skin = skinRepository.findByIdForUpdate(d.getSkin().getId())
                    .orElseThrow(() -> new RuntimeException("La skin de la orden ya no existe"));
            validarPublicacionDisponible(skin);
            skin.setStock(0);
            skin.setEstadoPublicacion(Skin.EstadoPublicacion.RESERVADA);
            skinRepository.save(skin);
        }
    }

    private void validarPublicacionDisponible(Skin skin) {
        if (skin == null || !Boolean.TRUE.equals(skin.getActive())) {
            throw new RuntimeException("La skin ya no está disponible");
        }
        if (Boolean.FALSE.equals(skin.getVendible())) {
            throw new RuntimeException("La skin no está habilitada para compra directa");
        }
        Skin.EstadoPublicacion estado = skin.getEstadoPublicacion();
        boolean publicadaConStock = (estado == null || estado == Skin.EstadoPublicacion.PUBLICADA)
                && skin.getStock() != null
                && skin.getStock() >= 1;
        boolean reservadaPorCheckout = estado == Skin.EstadoPublicacion.RESERVADA
                && skin.getStock() != null
                && skin.getStock() == 0;

        if (!publicadaConStock && !reservadaPorCheckout) {
            throw new RuntimeException("La skin ya fue reservada o vendida");
        }
    }

    private BrickPaymentResponse processLocalTestCardPayment(
            String email,
            Long orderId,
            TestCardPaymentRequest request,
            String cardNumber) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("La orden no pertenece al usuario autenticado");
        }
        if ("PAID".equals(order.getPaymentStatus())) {
            return approvedExistingPaymentResponse(order);
        }

        validatePayableOrder(order);
        validarPublicacionesDisponiblesParaPago(order);

        long paymentId = 9_000_000_000_000_000L + orderId;
        updateOrderPaymentState(order, paymentId, "PAID");

        return new BrickPaymentResponse(
                mapToOrderResponse(order),
                paymentId,
                PaymentStatus.APPROVED,
                "accredited",
                firstNonBlank(request.getPaymentMethodId(), inferPaymentMethodId(cardNumber)),
                "credit_card",
                null
        );
    }

    private BrickPaymentResponse approvedBalanceResponse(Order order) {
        return new BrickPaymentResponse(
                mapToOrderResponse(order),
                null,
                PaymentStatus.APPROVED,
                "accredited",
                "account_balance",
                "account_money",
                null
        );
    }

    private BrickPaymentResponse approvedExistingPaymentResponse(Order order) {
        return new BrickPaymentResponse(
                mapToOrderResponse(order),
                order.getMercadopagoPaymentId(),
                PaymentStatus.APPROVED,
                "already_paid",
                null,
                null,
                null
        );
    }

    private void acreditarRecargaSaldo(Order order) {
        double amountUsd = order.getPriceDifference() != null ? order.getPriceDifference() : 0.0;
        if (amountUsd <= 0) {
            throw new RuntimeException("La orden de recarga no tiene saldo para acreditar");
        }

        User user = order.getUser();
        double currentBalance = user.getSaldo() != null ? user.getSaldo() : 0.0;
        user.setSaldo(BigDecimal.valueOf(currentBalance + amountUsd)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue());
        userRepository.save(user);

        order.setSaldoAcreditado(true);
        order.setTradeStatus(TradeStatus.COMPLETED);
        orderRepository.save(order);
    }

    private List<OrderDetail> detallesOrdenados(Order order) {
        return order.getOrderDetails().stream()
                .filter((detail) -> detail.getSkin() != null && detail.getSkin().getId() != null)
                .sorted((a, b) -> a.getSkin().getId().compareTo(b.getSkin().getId()))
                .toList();
    }

    private void registrarVentasSiCorresponde(Order order, String previousStatus) {
        if ("PAID".equals(previousStatus)) return;

        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getSkin() == null) continue;
            eventoService.registrarVenta(
                    order,
                    detail.getSkin(),
                    detail.getQuantity() != null ? detail.getQuantity() : 1,
                    detail.getUnitPrice() != null ? detail.getUnitPrice() : 0.0
            );
        }
    }

    private boolean tieneSkinsBloqueadas(Order order) {
        for (OrderDetail d : order.getOrderDetails()) {
            if (d.getSkin() != null && d.getSkin().isLocked()) return true;
        }
        return false;
    }

    private boolean esPagoFallidoOCerrado(String status) {
        return "REJECTED".equals(status) ||
                "CANCELLED".equals(status) ||
                "REFUNDED".equals(status) ||
                "CHARGED_BACK".equals(status);
    }

    private void liberarPublicacionesReservadas(Order order) {
        if (order.getTradeStatus() == TradeStatus.COMPLETED ||
                order.getTradeStatus() == TradeStatus.BOT_SENT) {
            return;
        }
        for (OrderDetail d : order.getOrderDetails()) {
            if (d.getSkin() == null) continue;
            d.getSkin().setStock(1);
            d.getSkin().setActive(true);
            d.getSkin().setEstadoPublicacion(Skin.EstadoPublicacion.PUBLICADA);
        }
        if (order.getTradeStatus() == TradeStatus.WAITING_PAYMENT ||
                order.getTradeStatus() == TradeStatus.WAITING_DIFFERENCE) {
            order.setTradeStatus(TradeStatus.CANCELLED);
        }
        orderRepository.save(order);
    }

    private void crearItemsPendientesPorCompra(Order order) {
        for (OrderDetail d : order.getOrderDetails()) {
            Skin skin = d.getSkin();
            if (skin == null) continue;
            try {
                // Usamos el helper con REQUIRES_NEW para que un eventual duplicate-key
                // (si dos requests llegan al mismo tiempo) no rompa la transacción principal.
                inventarioItemCreadorHelper.crearSiNoExiste(
                        order.getUser(), order, skin, assetIdInventarioCompra(order, skin));
            } catch (Exception e) {
                // Si el item ya existía o hubo un conflicto, no es crítico para el pago.
                System.err.println("[PAYMENT] No se pudo crear item inventario para orden "
                        + order.getId() + ": " + e.getMessage());
            }
        }
    }

    private String assetIdInventarioCompra(Order order, Skin skin) {
        if (skin.getSteamAssetId() != null && !skin.getSteamAssetId().isBlank()) {
            return skin.getSteamAssetId();
        }
        return "PURCHASE-" + order.getId() + "-" + skin.getId();
    }

    private void crearBotOrderParaCompra(Order order) {
        List<String> assetIds = new ArrayList<>();
        // En PURCHASE, los assetIds reales son del bot.
        // Si la publicación nació desde inventario, ya guardamos el assetId real.
        // Las publicaciones admin pueden no tenerlo; en ese caso mantenemos el
        // identificador lógico para el mock.
        for (OrderDetail d : order.getOrderDetails()) {
            if (d.getSkin() != null) {
                String steamAssetId = d.getSkin().getSteamAssetId();
                String assetId = (steamAssetId != null && !steamAssetId.isBlank())
                        ? steamAssetId
                        : "SKIN-" + d.getSkin().getId();
                int quantity = d.getQuantity() != null ? d.getQuantity() : 1;
                for (int i = 0; i < quantity; i++) {
                    assetIds.add(assetId);
                }
            }
        }

        BotTradeOrdersFileService.BotOrder bo = new BotTradeOrdersFileService.BotOrder();
        bo.orderId = order.getId();
        bo.operationType = OperationType.PURCHASE.name();
        bo.status = "PAID_READY_TO_SEND";
        bo.direction = "BOT_TO_USER";
        bo.partnerSteamId64 = order.getUser().getSteamId64();
        bo.partnerTradeUrl = order.getUser().getTradeUrl();
        bo.assetIds = assetIds;
        bo.mockMode = "true";
        botFileService.upsert(bo);
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

    private void ensureTestCredentials() {
        if (!accessToken.startsWith("TEST-") || !publicKey.startsWith("TEST-")) {
            throw new RuntimeException("El pago con tarjeta de prueba solo funciona con credenciales TEST de Mercado Pago");
        }
    }

    private boolean hasPlaceholderTestCredentials() {
        return isPlaceholderCredential(accessToken) || isPlaceholderCredential(publicKey);
    }

    private boolean isPlaceholderCredential(String credential) {
        return credential != null && credential.matches("TEST-[0-]+");
    }

    private void validateTestCardData(TestCardPaymentRequest request, String authenticatedEmail) {
        Integer expirationMonth = request.getExpirationMonth();
        Integer expirationYear = normalizeExpirationYear(request.getExpirationYear());
        if (expirationMonth == null || expirationMonth < 1 || expirationMonth > 12 || expirationYear == null) {
            throw new RuntimeException("La fecha de vencimiento de la tarjeta es inválida");
        }

        LocalDateTime now = LocalDateTime.now();
        if (expirationYear < now.getYear()
                || (expirationYear == now.getYear() && expirationMonth < now.getMonthValue())) {
            throw new RuntimeException("La tarjeta está vencida");
        }

        String securityCode = request.getSecurityCode() != null ? request.getSecurityCode().trim() : "";
        if (!securityCode.matches("\\d{3,4}")) {
            throw new RuntimeException("El código de seguridad debe tener 3 o 4 números");
        }
        if (request.getCardholderName() == null || request.getCardholderName().trim().length() < 2) {
            throw new RuntimeException("Ingresá el nombre del titular de la tarjeta");
        }

        String documentNumber = request.getDocumentNumber() != null
                ? request.getDocumentNumber().replaceAll("\\D", "")
                : "";
        if (!documentNumber.matches("\\d{7,9}")) {
            throw new RuntimeException("El DNI debe tener entre 7 y 9 números");
        }

        String payerEmail = firstNonBlank(request.getEmail(), authenticatedEmail);
        if (payerEmail == null || !payerEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new RuntimeException("Ingresá un e-mail válido para el pago");
        }
    }

    private String createTestCardToken(TestCardPaymentRequest request, String cardNumber) {
        Integer expirationMonth = request.getExpirationMonth();
        Integer expirationYear = normalizeExpirationYear(request.getExpirationYear());
        if (expirationMonth == null || expirationMonth < 1 || expirationMonth > 12 || expirationYear == null) {
            throw new RuntimeException("La fecha de vencimiento de la tarjeta de prueba es inválida");
        }
        if (request.getSecurityCode() == null || request.getSecurityCode().isBlank()) {
            throw new RuntimeException("Falta el código de seguridad de la tarjeta de prueba");
        }

        String url = UriComponentsBuilder
                .fromUriString("https://api.mercadopago.com/v1/card_tokens")
                .queryParam("public_key", publicKey)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("card_number", cardNumber);
        body.put("expiration_month", expirationMonth);
        body.put("expiration_year", expirationYear);
        body.put("security_code", request.getSecurityCode().trim());
        body.put("cardholder", Map.of(
                "name", firstNonBlank(request.getCardholderName(), "APRO"),
                "identification", Map.of(
                        "type", firstNonBlank(request.getDocumentType(), "DNI"),
                        "number", firstNonBlank(request.getDocumentNumber(), "12345678")
                )
        ));

        ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        Object token = response.getBody() != null ? response.getBody().get("id") : null;
        if (token == null || token.toString().isBlank()) {
            throw new RuntimeException("Mercado Pago no devolvió token para la tarjeta de prueba");
        }

        return token.toString();
    }

    private String normalizeCardNumber(String cardNumber) {
        return cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
    }

    private Integer normalizeExpirationYear(Integer expirationYear) {
        if (expirationYear == null) return null;
        return expirationYear < 100 ? 2000 + expirationYear : expirationYear;
    }

    private String inferPaymentMethodId(String cardNumber) {
        if (cardNumber.startsWith("4")) return "visa";
        if (cardNumber.startsWith("5")) return "master";
        if (cardNumber.startsWith("3")) return "amex";
        return null;
    }

    private void validatePayableOrder(Order order) {
        if (order.getOperationType() == OperationType.SALE || order.getOperationType() == OperationType.RETURN) {
            throw new RuntimeException("Las operaciones " + order.getOperationType() + " no se pagan por Mercado Pago");
        }
        if (order.getOperationType() == OperationType.EXCHANGE
                && (order.getPriceDifference() == null || order.getPriceDifference() <= 0)) {
            throw new RuntimeException("Este intercambio no tiene diferencia positiva para pagar");
        }
        if (paymentAmount(order) <= 0) {
            throw new RuntimeException(
                    "La orden " + order.getId() + " tiene totalFinal inválido para pagar: "
                            + order.getTotalFinal() + ". Usá el order_id devuelto por la preferencia más reciente "
                            + "y verificá que la publicación tenga precio mayor a 0."
            );
        }
    }

    private double paymentAmount(Order order) {
        if (order.getOperationType() == OperationType.EXCHANGE) {
            return order.getPriceDifference() != null ? Math.max(order.getPriceDifference(), 0.0) : 0.0;
        }
        return order.getTotalFinal() != null ? order.getTotalFinal() : 0.0;
    }

    private double mercadoPagoAmount(Order order) {
        double amount = paymentAmount(order);
        if (order.getOperationType() == OperationType.BALANCE_TOP_UP) {
            return amount;
        }
        return roundMoney(amount * usdToArs);
    }

    private double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
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

    private boolean isPublicHttpsBackendUrl() {
        return backendUrl != null && backendUrl.startsWith("https://");
    }

    private String checkoutUrlFor(Preference preference) {
        if (isSandboxCheckout()) {
            return firstNonBlank(preference.getSandboxInitPoint(), preference.getInitPoint());
        }
        return firstNonBlank(preference.getInitPoint(), preference.getSandboxInitPoint());
    }

    private boolean isSandboxCheckout() {
        return hasTestCredentials();
    }

    private boolean hasTestCredentials() {
        return accessToken.startsWith("TEST-") && publicKey.startsWith("TEST-");
    }

    private String effectiveCheckoutMode() {
        return isSandboxCheckout() ? "sandbox" : "production";
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

    private String paymentStatusToMercadoPagoStatus(String status) {
        if ("PAID".equals(status)) return PaymentStatus.APPROVED;
        if ("REJECTED".equals(status)) return PaymentStatus.REJECTED;
        if ("CANCELLED".equals(status)) return PaymentStatus.CANCELLED;
        if ("REFUNDED".equals(status)) return PaymentStatus.REFUNDED;
        if ("PENDING_PAYMENT".equals(status)) return PaymentStatus.PENDING;
        if ("IN_PROCESS".equals(status)) return PaymentStatus.IN_PROCESS;
        return status != null ? status.toLowerCase() : "unknown";
    }

    private String buildDescription(Order order) {
        if (order.getOperationType() == OperationType.BALANCE_TOP_UP) {
            return "Recarga de saldo de intercambio";
        }
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
        response.setOperationType(order.getOperationType() != null ? order.getOperationType().name() : null);
        response.setTradeStatus(order.getTradeStatus() != null ? order.getTradeStatus().name() : null);
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
        response.setLocked(detail.getSkin() != null && detail.getSkin().isLocked());
        response.setLockedUntil(detail.getSkin() != null ? detail.getSkin().getLockedUntil() : null);
        response.setSecondsUntilUnlock(detail.getSkin() != null ? detail.getSkin().getSecondsUntilUnlock() : 0L);
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
