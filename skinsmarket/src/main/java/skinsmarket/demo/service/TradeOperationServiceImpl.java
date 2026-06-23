package skinsmarket.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skinsmarket.demo.controller.order.ExchangeRequest;
import skinsmarket.demo.controller.order.ExchangeQuoteResponse;
import skinsmarket.demo.controller.order.OperationStatusResponse;
import skinsmarket.demo.controller.order.OrderDetailResponse;
import skinsmarket.demo.controller.order.SaleRequest;
import skinsmarket.demo.entity.*;
import skinsmarket.demo.repository.InventarioItemRepository;
import skinsmarket.demo.repository.OrderRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.utils.TradeProfileValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementación del servicio de operaciones (venta / intercambio / cancelación).
 *
 * NO toca el flujo de compra clásico (sigue en OrderService + PaymentService),
 * pero después de cada compra exitosa (PAID), el PaymentService modificado
 * llama a este service vía createBotOrderForPurchase() para que el bot prepare
 * la entrega de las skins compradas.
 *
 * El precio "de mercado" de las skins del USER en un intercambio se calcula
 * con calcularValorSkinsUsuario(): busca el precio promedio del marketplace
 * para el mismo marketHashName, con default $1.00 si no hay publicaciones.
 */
@Service
public class TradeOperationServiceImpl implements TradeOperationService {

    /** Precio por defecto para skins del USER sin referencia en el marketplace. */
    private static final double PRECIO_DEFAULT_SKIN = 1.0;

    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InventarioItemRepository inventarioItemRepository;
    @Autowired private SkinRepository skinRepository;
    @Autowired private BotTradeOrdersFileService botFileService;

    private final ObjectMapper mapper = new ObjectMapper();

    // =========================================================================
    // VENTA — USER deposita skins al bot
    // =========================================================================

    @Override
    @Transactional
    public OperationStatusResponse createSale(String email, SaleRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        TradeProfileValidator.requireTradeUrl(user, "vender");
        TradeProfileValidator.requireAliasCobro(user, "vender");

        if (user.getSteamId64() == null || user.getSteamId64().isBlank()) {
            throw new RuntimeException("Debés configurar tu SteamID64 antes de vender");
        }
        if (request.getInventarioItemIds() == null || request.getInventarioItemIds().isEmpty()) {
            throw new RuntimeException("Tenés que seleccionar al menos un item para vender");
        }
        if (request.getPrecioOfrecido() == null || request.getPrecioOfrecido() <= 0) {
            throw new RuntimeException("El precio ofrecido debe ser mayor a 0");
        }

        // Validar que cada item pertenezca al user y esté tradeable
        List<String> assetIds = new ArrayList<>();
        List<InventarioItem> items = new ArrayList<>();

        for (Long invItemId : request.getInventarioItemIds()) {
            InventarioItem item = inventarioItemRepository.findById(invItemId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Item de inventario no encontrado: " + invItemId));

            if (!item.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("El item " + invItemId + " no es tuyo");
            }
            if (Boolean.TRUE.equals(item.getPublicado())) {
                throw new RuntimeException(
                        "El item '" + item.getName() + "' ya está publicado a la venta");
            }
            if (Boolean.FALSE.equals(item.getTradable())) {
                throw new RuntimeException(
                        "El item '" + item.getName() + "' no es tradeable (trade lock o intransferible)");
            }
            // Verificar que no esté siendo usado en otra operación activa
            if (!orderRepository.findActiveOrdersWithAssetId(item.getAssetId()).isEmpty()) {
                throw new RuntimeException(
                        "El item '" + item.getName() + "' ya está reservado en otra operación activa");
            }

            assetIds.add(item.getAssetId());
            items.add(item);
        }

        // Crear la Order de venta
        Order order = new Order();
        order.setUser(user);
        order.setDate(LocalDateTime.now());
        order.setOperationType(OperationType.SALE);
        order.setTradeStatus(TradeStatus.WAITING_USER_TRADE);
        order.setPaymentStatus("N/A");
        order.setTotalPrice(request.getPrecioOfrecido());
        order.setTotalFinal(request.getPrecioOfrecido());
        order.setDescuentoAplicado(0.0);
        order.setUserSkinAssetIds(serializeAssetIds(assetIds));
        order.setExpectedAssetIds(serializeAssetIds(assetIds));

        orderRepository.save(order);

        // Crear entrada en orders.json para el bot — espera USER → BOT
        BotTradeOrdersFileService.BotOrder bo = new BotTradeOrdersFileService.BotOrder();
        bo.orderId = order.getId();
        bo.operationType = OperationType.SALE.name();
        bo.status = "WAITING_USER_TRADE";
        bo.direction = "USER_TO_BOT";
        bo.partnerSteamId64 = user.getSteamId64();
        bo.partnerTradeUrl = user.getTradeUrl();
        bo.assetIds = assetIds;
        bo.mockMode = "true";
        botFileService.upsert(bo);

        return mapToResponse(order);
    }

    // =========================================================================
    // INTERCAMBIO
    // =========================================================================

    @Override
    @Transactional
    public OperationStatusResponse createExchange(String email, ExchangeRequest request) {
        ExchangeCalculation calc = calcularIntercambio(email, request);
        User user = calc.user();
        double diferenciaAPagar = roundMoney(Math.max(calc.diferencia(), 0.0));
        double saldoActual = roundMoney(user.getSaldo() != null ? user.getSaldo() : 0.0);

        if (diferenciaAPagar > saldoActual) {
            double faltante = diferenciaAPagar - saldoActual;
            throw new RuntimeException(String.format(
                    "Saldo insuficiente. Te faltan $%.2f USD para realizar el intercambio.",
                    faltante));
        }

        if (diferenciaAPagar > 0) {
            user.setSaldo(roundMoney(saldoActual - diferenciaAPagar));
            userRepository.save(user);
        }

        // Crear la Order de intercambio
        Order order = new Order();
        order.setUser(user);
        order.setDate(LocalDateTime.now());
        order.setOperationType(OperationType.EXCHANGE);
        order.setTradeStatus(TradeStatus.WAITING_USER_TRADE);
        order.setPaymentStatus(diferenciaAPagar > 0 ? "PAID_WITH_BALANCE" : "N/A");
        order.setTotalPrice(calc.valorMarketplace());
        order.setTotalFinal(diferenciaAPagar);
        order.setDescuentoAplicado(0.0);
        order.setPriceDifference(roundMoney(calc.diferencia()));
        order.setUserSkinAssetIds(serializeAssetIds(calc.userAssetIds()));

        // Agregar las skins del marketplace como detalles (se reservan)
        for (Skin skin : calc.skinsMarketplace()) {
            skin.setStock(0);  // publicación única reservada
            skin.setEstadoPublicacion(Skin.EstadoPublicacion.RESERVADA);
            skinRepository.save(skin);

            OrderDetail d = new OrderDetail();
            d.setSkin(skin);
            d.setQuantity(1);
            d.setUnitPrice(precioPromedioMarketplace(skin));
            order.addOrderDetail(d);
        }

        orderRepository.save(order);

        // Crear entrada en orders.json
        BotTradeOrdersFileService.BotOrder bo = new BotTradeOrdersFileService.BotOrder();
        bo.orderId = order.getId();
        bo.operationType = OperationType.EXCHANGE.name();
        bo.status = "WAITING_USER_TRADE";
        bo.direction = "USER_TO_BOT";
        bo.partnerSteamId64 = user.getSteamId64();
        bo.partnerTradeUrl = user.getTradeUrl();
        bo.assetIds = calc.userAssetIds();
        bo.mockMode = "true";
        botFileService.upsert(bo);

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeQuoteResponse quoteExchange(String email, ExchangeRequest request) {
        ExchangeCalculation calc = calcularIntercambio(email, request);
        return mapQuote(
                calc.valorMarketplace(),
                calc.valorUsuario(),
                calc.diferencia(),
                calc.user().getSaldo());
    }

    @Override
    @Transactional(readOnly = true)
    public double estimateInventoryItemPrice(InventarioItem item) {
        return precioPromedioMarketplace(item);
    }

    // =========================================================================
    // CANCELACIÓN — manejo de todos los casos
    // =========================================================================

    @Override
    @Transactional
    public OperationStatusResponse cancelOperation(String email, Long orderId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("La orden no es tuya");
        }

        TradeStatus current = order.getTradeStatus();

        // CASO 4.4: bot ya envió la skin final — no se puede cancelar
        if (current == TradeStatus.COMPLETED ||
            current == TradeStatus.RETURNED ||
            current == TradeStatus.BOT_SENT) {
            throw new RuntimeException(
                    "No se puede cancelar: el trade ya fue enviado o completado. " +
                    "Contactá soporte si necesitás ayuda.");
        }

        // CASO 4.1: cancela ANTES de entregar skins → cancelación simple
        if (current == TradeStatus.WAITING_PAYMENT ||
            current == TradeStatus.WAITING_USER_TRADE ||
            current == TradeStatus.WAITING_DIFFERENCE) {

            order.setTradeStatus(TradeStatus.CANCELLED);

            reintegrarSaldoPagado(order);

            // Liberar stock de skins del marketplace (intercambio/compra)
            for (OrderDetail d : order.getOrderDetails()) {
                Skin s = d.getSkin();
                if (s != null) {
                    s.setStock(1);
                    s.setEstadoPublicacion(Skin.EstadoPublicacion.PUBLICADA);
                    s.setActive(true);
                    skinRepository.save(s);
                }
            }
            orderRepository.save(order);

            // Borrar entrada del bot
            botFileService.delete(order.getId());

            return mapToResponse(order, "Operación cancelada. No habías entregado skins.");
        }

        // CASO 4.2 y 4.3: el USER YA entregó skins → generar devolución
        if (current == TradeStatus.USER_TRADE_RECEIVED ||
            current == TradeStatus.PREPARING_TRADE) {

            descontarSaldoSiFueAcreditado(order);
            order.setTradeStatus(TradeStatus.RETURN_PENDING);
            orderRepository.save(order);

            // Crear una Order de tipo RETURN vinculada a esta
            Order returnOrder = new Order();
            returnOrder.setUser(user);
            returnOrder.setDate(LocalDateTime.now());
            returnOrder.setOperationType(OperationType.RETURN);
            returnOrder.setTradeStatus(TradeStatus.RETURN_PENDING);
            returnOrder.setPaymentStatus("N/A");
            returnOrder.setTotalPrice(0.0);
            returnOrder.setTotalFinal(0.0);
            returnOrder.setDescuentoAplicado(0.0);
            returnOrder.setRelatedOrderId(order.getId());
            // Devolvemos exactamente los assetIds que el user había entregado
            returnOrder.setExpectedAssetIds(order.getUserSkinAssetIds());
            orderRepository.save(returnOrder);

            // Liberar stock de las skins del marketplace que estaban reservadas
            for (OrderDetail d : order.getOrderDetails()) {
                Skin s = d.getSkin();
                if (s != null) {
                    s.setStock(1);
                    s.setEstadoPublicacion(Skin.EstadoPublicacion.PUBLICADA);
                    s.setActive(true);
                    skinRepository.save(s);
                }
            }

            // Crear entrada en orders.json para el bot — BOT → USER
            BotTradeOrdersFileService.BotOrder bo = new BotTradeOrdersFileService.BotOrder();
            bo.orderId = returnOrder.getId();
            bo.operationType = OperationType.RETURN.name();
            bo.status = "PENDING";
            bo.direction = "BOT_TO_USER";
            bo.partnerSteamId64 = user.getSteamId64();
            bo.partnerTradeUrl = user.getTradeUrl();
            bo.assetIds = deserializeAssetIds(order.getUserSkinAssetIds());
            bo.mockMode = "true";
            botFileService.upsert(bo);

            return mapToResponse(returnOrder,
                    "Operación cancelada. Se generó una devolución (orden #" +
                    returnOrder.getId() + "). El bot te enviará tus skins.");
        }

        // Cualquier otro estado: ya está cerrada
        throw new RuntimeException(
                "La operación está en estado " + current + " y no se puede cancelar.");
    }

    // =========================================================================
    // Bot confirma trade recibido
    // =========================================================================

    @Override
    @Transactional
    public OperationStatusResponse markUserTradeReceived(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));

        if (order.getTradeStatus() != TradeStatus.WAITING_USER_TRADE) {
            throw new RuntimeException(
                    "La orden no está esperando entrega de skins (estado actual: " +
                    order.getTradeStatus() + ")");
        }

        order.setTradeStatus(TradeStatus.USER_TRADE_RECEIVED);
        orderRepository.save(order);

        // Si es VENTA: pasar a PREPARING_TRADE (pago/saldo). Mock luego lo completa.
        // Si es EXCHANGE con diferencia > 0: pasar a WAITING_DIFFERENCE.
        // Si es EXCHANGE con diferencia <= 0: pasar a PREPARING_TRADE directamente.
        if (order.getOperationType() == OperationType.SALE) {
            order.setTradeStatus(TradeStatus.PREPARING_TRADE);
        } else if (order.getOperationType() == OperationType.EXCHANGE) {
            if (order.getPriceDifference() != null && order.getPriceDifference() > 0 &&
                    !"PAID_WITH_BALANCE".equals(order.getPaymentStatus())) {
                order.setTradeStatus(TradeStatus.WAITING_DIFFERENCE);
            } else {
                order.setTradeStatus(TradeStatus.PREPARING_TRADE);
                acreditarSaldoSiCorresponde(order);
            }
        }
        orderRepository.save(order);

        // Actualizar archivo bot
        botFileService.updateStatus(orderId, order.getTradeStatus().name());

        return mapToResponse(order);
    }

    // =========================================================================
    // Consultas
    // =========================================================================

    @Override
    @Transactional
    public OperationStatusResponse getStatus(String email, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada"));
        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("La orden no es tuya");
        }
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public List<OperationStatusResponse> listMyOperations(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        List<Order> orders = orderRepository.findDetailedByUserIdOrderByDateDesc(user.getId());
        List<OperationStatusResponse> out = new ArrayList<>();
        for (Order o : orders) out.add(mapToResponse(o));
        return out;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Valida el intercambio y calcula valores usando publicaciones comparables:
     * misma skin del catálogo, mismo desgaste y mismo StatTrak.
     */
    private ExchangeCalculation calcularIntercambio(String email, ExchangeRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        TradeProfileValidator.requireTradeUrl(user, "intercambiar");

        if (user.getSteamId64() == null || user.getSteamId64().isBlank()) {
            throw new RuntimeException("Debés configurar tu SteamID64 antes de intercambiar");
        }
        if (request.getInventarioItemIds() == null || request.getInventarioItemIds().isEmpty()) {
            throw new RuntimeException("Tenés que ofrecer al menos una skin propia");
        }
        if (request.getSkinIds() == null || request.getSkinIds().isEmpty()) {
            throw new RuntimeException("Tenés que pedir al menos una skin del marketplace");
        }

        List<String> userAssetIds = new ArrayList<>();
        double valorUsuario = 0.0;
        for (Long invItemId : request.getInventarioItemIds()) {
            InventarioItem item = inventarioItemRepository.findById(invItemId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Item de inventario no encontrado: " + invItemId));
            if (!item.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("El item " + invItemId + " no es tuyo");
            }
            if (Boolean.FALSE.equals(item.getTradable())) {
                throw new RuntimeException("El item '" + item.getName() + "' no es tradeable");
            }
            if (Boolean.TRUE.equals(item.getPublicado())) {
                throw new RuntimeException("El item '" + item.getName() + "' ya está publicado");
            }
            if (item.getCatalogo() == null) {
                throw new RuntimeException("El item '" + item.getName() + "' no está asociado al catálogo y no se puede cotizar");
            }
            if (!orderRepository.findActiveOrdersWithAssetId(item.getAssetId()).isEmpty()) {
                throw new RuntimeException("El item '" + item.getName() + "' está en otra operación activa");
            }
            userAssetIds.add(item.getAssetId());
            valorUsuario += precioPromedioMarketplace(item);
        }

        List<Skin> skinsMarketplace = new ArrayList<>();
        double valorMarketplace = 0.0;
        for (Long skinId : request.getSkinIds()) {
            Skin skin = skinRepository.findById(skinId)
                    .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + skinId));
            if (!Boolean.TRUE.equals(skin.getActive())) {
                throw new RuntimeException("La skin '" + skin.getName() + "' no está disponible");
            }
            if (skin.getEstadoPublicacion() != null &&
                    skin.getEstadoPublicacion() != Skin.EstadoPublicacion.PUBLICADA) {
                throw new RuntimeException("La skin '" + skin.getName() + "' no está disponible");
            }
            if (skin.getStock() == null || skin.getStock() < 1) {
                throw new RuntimeException("La skin '" + skin.getName() + "' ya fue reservada o vendida");
            }
            if (Boolean.FALSE.equals(skin.getIntercambiable()) || Boolean.TRUE.equals(skin.getVendible())) {
                throw new RuntimeException("La skin '" + skin.getName() + "' no está habilitada para intercambio");
            }
            if (skin.getCatalogo() == null) {
                throw new RuntimeException("La skin '" + skin.getName() + "' no está asociada al catálogo y no se puede cotizar");
            }
            if (skin.isLocked()) {
                throw new RuntimeException(
                        "La skin '" + skin.getName() + "' está bloqueada hasta " +
                                skin.getLockedUntil() + " y todavía no se puede intercambiar");
            }
            if (skin.getVendedor() != null && skin.getVendedor().getEmail().equals(email)) {
                throw new RuntimeException("No podés intercambiar por una skin que vos mismo publicaste");
            }
            skinsMarketplace.add(skin);
            valorMarketplace += precioPromedioMarketplace(skin);
        }

        double diferencia = valorMarketplace - valorUsuario;
        return new ExchangeCalculation(
                user, userAssetIds, skinsMarketplace, valorMarketplace, valorUsuario, diferencia);
    }

    /**
     * Calcula el precio promedio de publicaciones equivalentes. Si no hay
     * comparables, usa el precio de la publicación puntual como fallback.
     */
    private double precioPromedioMarketplace(Skin skin) {
        if (skin == null) return PRECIO_DEFAULT_SKIN;
        double fallback = skin.getFinalPrice() != null ? skin.getFinalPrice() : PRECIO_DEFAULT_SKIN;
        return precioPromedioMarketplace(
                skin.getCatalogo() != null ? skin.getCatalogo().getId() : null,
                skin.getExterior(),
                Boolean.TRUE.equals(skin.getStattrak()),
                fallback);
    }

    private double precioPromedioMarketplace(InventarioItem item) {
        if (item == null) return PRECIO_DEFAULT_SKIN;
        return precioPromedioMarketplace(
                item.getCatalogo() != null ? item.getCatalogo().getId() : null,
                exteriorDesdeMarketHashName(item.getMarketHashName()),
                esStatTrak(item.getMarketHashName()),
                PRECIO_DEFAULT_SKIN);
    }

    private double precioPromedioMarketplace(
            Long catalogoId, Skin.Exterior exterior, boolean stattrak, double fallback) {
        List<Skin> publicaciones = skinRepository.findPublicacionesComparables(
                catalogoId, exterior, stattrak);
        if (publicaciones.isEmpty()) return fallback;
        double sum = 0;
        for (Skin s : publicaciones) sum += s.getFinalPrice();
        return sum / publicaciones.size();
    }

    private Skin.Exterior exteriorDesdeMarketHashName(String marketHashName) {
        if (marketHashName == null) return null;
        if (marketHashName.contains("(Factory New)")) return Skin.Exterior.RECIEN_FABRICADO;
        if (marketHashName.contains("(Minimal Wear)")) return Skin.Exterior.CASI_NUEVO;
        if (marketHashName.contains("(Field-Tested)")) return Skin.Exterior.ALGO_DESGASTADO;
        if (marketHashName.contains("(Well-Worn)")) return Skin.Exterior.BASTANTE_DESGASTADO;
        if (marketHashName.contains("(Battle-Scarred)")) return Skin.Exterior.DEPLORABLE;
        return null;
    }

    private boolean esStatTrak(String marketHashName) {
        return marketHashName != null && marketHashName.toLowerCase().contains("stattrak");
    }

    private void acreditarSaldoSiCorresponde(Order order) {
        if (order.getOperationType() != OperationType.EXCHANGE) return;
        if (Boolean.TRUE.equals(order.getSaldoAcreditado())) return;
        if (order.getPriceDifference() == null || order.getPriceDifference() >= 0) return;

        User user = order.getUser();
        double saldoActual = user.getSaldo() != null ? user.getSaldo() : 0.0;
        user.setSaldo(saldoActual + Math.abs(order.getPriceDifference()));
        order.setSaldoAcreditado(true);
        userRepository.save(user);
    }

    private void descontarSaldoSiFueAcreditado(Order order) {
        if (!Boolean.TRUE.equals(order.getSaldoAcreditado())) return;
        if (order.getPriceDifference() == null || order.getPriceDifference() >= 0) return;

        User user = order.getUser();
        double saldoActual = user.getSaldo() != null ? user.getSaldo() : 0.0;
        user.setSaldo(Math.max(0.0, saldoActual - Math.abs(order.getPriceDifference())));
        order.setSaldoAcreditado(false);
        userRepository.save(user);
    }

    private ExchangeQuoteResponse mapQuote(
            double valorMarketplace,
            double valorUsuario,
            double diferencia,
            Double saldoActualValue) {
        ExchangeQuoteResponse r = new ExchangeQuoteResponse();
        double saldoActual = roundMoney(saldoActualValue != null ? saldoActualValue : 0.0);
        double diferenciaRedondeada = roundMoney(diferencia);
        double montoAPagar = roundMoney(Math.max(diferenciaRedondeada, 0.0));
        double saldoARecibir = roundMoney(Math.max(-diferenciaRedondeada, 0.0));
        r.setValorMarketplace(roundMoney(valorMarketplace));
        r.setValorUsuario(roundMoney(valorUsuario));
        r.setDiferencia(diferenciaRedondeada);
        r.setMontoAPagar(montoAPagar);
        r.setSaldoARecibir(saldoARecibir);
        r.setSaldoDisponible(saldoActual);
        r.setSaldoFaltante(roundMoney(Math.max(montoAPagar - saldoActual, 0.0)));
        r.setSaldoRestante(roundMoney(Math.max(saldoActual - montoAPagar, 0.0) + saldoARecibir));
        if (diferenciaRedondeada > 0) {
            r.setResultado("PAGA_DIFERENCIA");
        } else if (diferenciaRedondeada < 0) {
            r.setResultado("RECIBE_SALDO");
        } else {
            r.setResultado("INTERCAMBIO_DIRECTO");
        }
        return r;
    }

    private void reintegrarSaldoPagado(Order order) {
        if (!"PAID_WITH_BALANCE".equals(order.getPaymentStatus())) return;
        if (order.getPriceDifference() == null || order.getPriceDifference() <= 0) return;

        User user = order.getUser();
        double saldoActual = user.getSaldo() != null ? user.getSaldo() : 0.0;
        user.setSaldo(roundMoney(saldoActual + order.getPriceDifference()));
        order.setPaymentStatus("REFUNDED_TO_BALANCE");
        userRepository.save(user);
    }

    private double roundMoney(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String serializeAssetIds(List<String> ids) {
        try { return mapper.writeValueAsString(ids); }
        catch (Exception e) { return "[]"; }
    }

    private List<String> deserializeAssetIds(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return mapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    private OperationStatusResponse mapToResponse(Order order) {
        return mapToResponse(order, mensajePorEstado(order.getTradeStatus()));
    }

    private record ExchangeCalculation(
            User user,
            List<String> userAssetIds,
            List<Skin> skinsMarketplace,
            double valorMarketplace,
            double valorUsuario,
            double diferencia) {
    }

    private OperationStatusResponse mapToResponse(Order order, String mensaje) {
        OperationStatusResponse r = new OperationStatusResponse();
        r.setId(order.getId());
        r.setOperationType(order.getOperationType() != null ? order.getOperationType().name() : null);
        r.setTradeStatus(order.getTradeStatus() != null ? order.getTradeStatus().name() : null);
        r.setPaymentStatus(order.getPaymentStatus());
        r.setEmail(order.getUser().getEmail());
        r.setDate(order.getDate());
        r.setTotalPrice(order.getTotalPrice());
        r.setTotalFinal(order.getTotalFinal());
        r.setDescuentoAplicado(order.getDescuentoAplicado());
        r.setPriceDifference(order.getPriceDifference());
        r.setSaldoARecibir(order.getPriceDifference() != null && order.getPriceDifference() < 0
                ? Math.abs(order.getPriceDifference())
                : 0.0);
        r.setSaldoAcreditado(Boolean.TRUE.equals(order.getSaldoAcreditado()));
        r.setBotTradeOfferId(order.getBotTradeOfferId());
        r.setExpectedAssetIds(order.getExpectedAssetIds());
        r.setUserSkinAssetIds(order.getUserSkinAssetIds());
        r.setMercadopagoPreferenceId(order.getMercadopagoPreferenceId());
        r.setMercadopagoPaymentId(order.getMercadopagoPaymentId());
        r.setRelatedOrderId(order.getRelatedOrderId());
        r.setMensajeEstado(mensaje);

        List<OrderDetailResponse> details = new ArrayList<>();
        for (OrderDetail d : order.getOrderDetails()) {
            OrderDetailResponse rd = new OrderDetailResponse();
            rd.setSkinId(d.getSkin() != null ? d.getSkin().getId() : null);
            rd.setSkinName(d.getSkin() != null ? d.getSkin().getName() : null);
            rd.setQuantity(d.getQuantity());
            rd.setUnitPrice(d.getUnitPrice());
            rd.setLocked(d.getSkin() != null && d.getSkin().isLocked());
            rd.setLockedUntil(d.getSkin() != null ? d.getSkin().getLockedUntil() : null);
            rd.setSecondsUntilUnlock(d.getSkin() != null ? d.getSkin().getSecondsUntilUnlock() : 0L);
            details.add(rd);
        }
        r.setOrderDetailResponses(details);
        return r;
    }

    private String mensajePorEstado(TradeStatus s) {
        if (s == null) return "Estado desconocido";
        return switch (s) {
            case WAITING_PAYMENT -> "Esperando confirmación de pago";
            case WAITING_UNLOCK -> "Reserva pagada. Esperando que la skin se desbloquee para enviarla por Steam.";
            case WAITING_USER_TRADE -> "Esperando que envíes la oferta de trade al bot";
            case USER_TRADE_RECEIVED -> "El bot recibió tus skins";
            case WAITING_DIFFERENCE -> "Esperando que pagues la diferencia";
            case PREPARING_TRADE -> "Preparando envío del bot";
            case BOT_SENT -> "El bot envió la oferta. Aceptala en Steam.";
            case COMPLETED -> "Operación completada exitosamente";
            case CANCELLED -> "Operación cancelada";
            case EXPIRED -> "La oferta expiró en Steam";
            case FAILED -> "La operación falló";
            case RETURN_PENDING -> "Pendiente de devolución";
            case RETURN_SENT -> "El bot envió la oferta de devolución";
            case RETURNED -> "Skins devueltas exitosamente";
            case RETURN_FAILED -> "La devolución falló";
        };
    }
}
