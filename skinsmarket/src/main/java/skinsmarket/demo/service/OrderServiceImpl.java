package skinsmarket.demo.service;

import skinsmarket.demo.controller.order.OrderDetailRequest;
import skinsmarket.demo.controller.order.OrderDetailResponse;
import skinsmarket.demo.controller.order.OrderRequest;
import skinsmarket.demo.controller.order.OrderResponse;
import skinsmarket.demo.entity.*;
import skinsmarket.demo.exception.NoStockAvailableException;
import skinsmarket.demo.exception.PropietarioSkinException;
import skinsmarket.demo.repository.CarritoRepository;
import skinsmarket.demo.repository.CuponRepository;
import skinsmarket.demo.repository.OrderRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderServiceImpl — flujo de COMPRA clásico, integrado ahora con
 * operationType=PURCHASE y tradeStatus=WAITING_PAYMENT.
 *
 * CAMBIOS sobre la versión anterior:
 *   - Cada Order creada se marca como PURCHASE y WAITING_PAYMENT
 *   - El MockTradeScheduler detecta órdenes PAID+WAITING_PAYMENT y avanza
 *     automáticamente el trade hasta COMPLETED.
 *
 * Mantengo el resto del comportamiento original.
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private SkinRepository skinRepository;
    @Autowired private CuponRepository cuponRepository;
    @Autowired private CarritoRepository carritoRepository;
    @Autowired private UserRepository userRepository;

    @Autowired private EventoService eventoService;

    @Override
    @Transactional
    public OrderResponse createOrderFromCarrito(String email, String codigoCupon)
            throws NoStockAvailableException, PropietarioSkinException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Carrito carrito = carritoRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("El usuario no tiene carrito"));

        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        List<OrderDetailRequest> items = carrito.getItems().stream()
                .map(item -> {
                    OrderDetailRequest d = new OrderDetailRequest();
                    d.setSkinId(item.getSkin().getId());
                    d.setQuantity(item.getCantidad());
                    return d;
                }).toList();

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItemList(items);
        orderRequest.setCodigoCupon(codigoCupon);

        return createOrder(user, orderRequest);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(User user, OrderRequest orderRequest)
            throws NoStockAvailableException, PropietarioSkinException {

        LocalDateTime now = LocalDateTime.now();

        if (orderRequest.getItemList() == null || orderRequest.getItemList().isEmpty()) {
            throw new RuntimeException("La orden debe contener al menos un ítem");
        }

        Order order = new Order();
        order.setUser(user);
        order.setDate(now);

        // ✨ NUEVO: marcar como compra y esperando pago de MP
        order.setOperationType(OperationType.PURCHASE);
        order.setTradeStatus(TradeStatus.WAITING_PAYMENT);

        List<OrderDetailResponse> detailResponses = new ArrayList<>();
        double totalPrice = 0.0;
        List<Object[]> ventasARegistrar = new ArrayList<>();

        for (OrderDetailRequest item : orderRequest.getItemList()) {
            Long skinId = item.getSkinId();
            int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
            if (quantity != 1) {
                throw new RuntimeException("Cada skin publicada es única. La cantidad debe ser 1");
            }
            Skin skin = skinRepository.findById(skinId)
                    .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + skinId));

            if (!skin.getActive()) {
                throw new RuntimeException("La skin '" + skin.getName() + "' ya no está disponible");
            }
            if (skin.getVendedor() != null
                    && skin.getVendedor().getEmail().equals(user.getEmail())) {
                throw new PropietarioSkinException();
            }
            if (skin.getStock() == null || skin.getStock() < 1) {
                throw new NoStockAvailableException();
            }

            skin.setStock(0);
            skinRepository.save(skin);

            double finalPrice = skin.getFinalPrice();

            OrderDetail detail = new OrderDetail();
            detail.setSkin(skin);
            detail.setQuantity(1);
            detail.setUnitPrice(finalPrice);
            order.addOrderDetail(detail);

            OrderDetailResponse detailResponse = new OrderDetailResponse();
            detailResponse.setSkinId(skinId);
            detailResponse.setSkinName(skin.getName());
            detailResponse.setQuantity(1);
            detailResponse.setUnitPrice(finalPrice);
            detailResponse.setLocked(skin.isLocked());
            detailResponse.setLockedUntil(skin.getLockedUntil());
            detailResponse.setSecondsUntilUnlock(skin.getSecondsUntilUnlock());
            detailResponses.add(detailResponse);

            totalPrice += finalPrice;

            ventasARegistrar.add(new Object[]{skin, 1, finalPrice});
        }

        double descuentoAplicado = 0.0;
        if (orderRequest.getCodigoCupon() != null && !orderRequest.getCodigoCupon().isBlank()) {
            Cupon cupon = cuponRepository.findByCodigo(orderRequest.getCodigoCupon())
                    .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

            if (!cupon.getActivo()) throw new RuntimeException("El cupón no está activo");
            if (cupon.getFechaVencimiento() != null
                    && cupon.getFechaVencimiento().isBefore(LocalDate.now())) {
                throw new RuntimeException("El cupón está vencido");
            }

            descuentoAplicado = cupon.getDescuento();
            if (!cupon.getMultiUso()) {
                cupon.setActivo(false);
                cuponRepository.save(cupon);
            }
        }

        double totalFinal = totalPrice * (1 - descuentoAplicado);
        order.setTotalPrice(totalPrice);
        order.setDescuentoAplicado(descuentoAplicado);
        order.setTotalFinal(totalFinal);
        orderRepository.save(order);

        for (Object[] venta : ventasARegistrar) {
            Skin skin = (Skin) venta[0];
            int cantidad = (int) venta[1];
            double precio = (double) venta[2];
            eventoService.registrarVenta(order, skin, cantidad, precio);
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setEmail(user.getEmail());
        response.setDate(now);
        response.setTotalPrice(totalPrice);
        response.setDescuentoAplicado(descuentoAplicado);
        response.setTotalFinal(totalFinal);
        response.setPaymentStatus(order.getPaymentStatus());
        response.setOperationType(order.getOperationType().name());
        response.setTradeStatus(order.getTradeStatus().name());
        response.setMercadopagoPreferenceId(order.getMercadopagoPreferenceId());
        response.setMercadopagoPaymentId(order.getMercadopagoPaymentId());
        response.setOrderDetailResponses(detailResponses);
        return response;
    }

    @Override
    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public List<OrderResponse> getOrdersForUser(User user) {
        List<Order> orders = orderRepository.findByUserIdOrderByDateDesc(user.getId());
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) responses.add(mapToOrderResponse(order));
        return responses;
    }

    @Override
    public OrderResponse getOrderById(Long id, String email) {
        return orderRepository.findById(id)
                .filter(o -> o.getUser().getEmail().equals(email))
                .map(this::mapToOrderResponse)
                .orElse(null);
    }

    private OrderResponse mapToOrderResponse(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setEmail(order.getUser().getEmail());
        resp.setDate(order.getDate());
        resp.setTotalPrice(order.getTotalPrice());
        resp.setDescuentoAplicado(order.getDescuentoAplicado());
        resp.setTotalFinal(order.getTotalFinal());
        resp.setPaymentStatus(order.getPaymentStatus());
        resp.setOperationType(order.getOperationType() != null ? order.getOperationType().name() : null);
        resp.setTradeStatus(order.getTradeStatus() != null ? order.getTradeStatus().name() : null);
        resp.setMercadopagoPreferenceId(order.getMercadopagoPreferenceId());
        resp.setMercadopagoPaymentId(order.getMercadopagoPaymentId());

        List<OrderDetailResponse> details = new ArrayList<>();
        for (OrderDetail od : order.getOrderDetails()) {
            OrderDetailResponse d = new OrderDetailResponse();
            d.setSkinId(od.getSkin() != null ? od.getSkin().getId() : null);
            d.setSkinName(od.getSkin() != null ? od.getSkin().getName() : null);
            d.setQuantity(od.getQuantity());
            d.setUnitPrice(od.getUnitPrice());
            d.setLocked(od.getSkin() != null && od.getSkin().isLocked());
            d.setLockedUntil(od.getSkin() != null ? od.getSkin().getLockedUntil() : null);
            d.setSecondsUntilUnlock(od.getSkin() != null ? od.getSkin().getSecondsUntilUnlock() : 0L);
            details.add(d);
        }
        resp.setOrderDetailResponses(details);
        return resp;
    }
}
