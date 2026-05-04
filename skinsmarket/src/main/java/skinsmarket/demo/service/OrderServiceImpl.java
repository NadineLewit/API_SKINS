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

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private SkinRepository skinRepository;
    @Autowired private CuponRepository cuponRepository;
    @Autowired private CarritoRepository carritoRepository;
    @Autowired private UserRepository userRepository;

    /** Servicio de eventos para registrar ventas (tracking). */
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

        List<OrderDetailResponse> detailResponses = new ArrayList<>();
        double totalPrice = 0.0;

        // Lista temporal para registrar eventos DESPUÉS de persistir la orden
        // (necesitamos el id de la orden y de la skin para vincular bien)
        List<Object[]> ventasARegistrar = new ArrayList<>();

        for (OrderDetailRequest item : orderRequest.getItemList()) {
            Long skinId = item.getSkinId();
            Skin skin = skinRepository.findById(skinId)
                    .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + skinId));

            if (!skin.getActive()) {
                throw new RuntimeException("La skin '" + skin.getName() + "' ya no está disponible");
            }
            if (skin.getVendedor() != null
                    && skin.getVendedor().getEmail().equals(user.getEmail())) {
                throw new PropietarioSkinException();
            }
            if (item.getQuantity() > skin.getStock()) {
                throw new NoStockAvailableException();
            }

            skin.setStock(skin.getStock() - item.getQuantity());
            skinRepository.save(skin);

            double finalPrice = skin.getFinalPrice();

            OrderDetail detail = new OrderDetail();
            detail.setSkin(skin);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(finalPrice);
            order.addOrderDetail(detail);

            OrderDetailResponse detailResponse = new OrderDetailResponse();
            detailResponse.setSkinId(skinId);
            detailResponse.setSkinName(skin.getName());
            detailResponse.setQuantity(item.getQuantity());
            detailResponse.setUnitPrice(finalPrice);
            detailResponses.add(detailResponse);

            totalPrice += item.getQuantity() * finalPrice;

            // Encolamos para registrar como evento después
            ventasARegistrar.add(new Object[]{skin, item.getQuantity(), finalPrice});
        }

        // Cupón
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

        // 📊 Registrar eventos SALE para el ranking de vendedores y skins más vendidas.
        // Hacemos esto al final, después de que la orden está persistida.
        // El EventoService es defensivo y no propaga excepciones, así que si
        // falla acá no rompe la orden ya creada.
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

        List<OrderDetailResponse> details = new ArrayList<>();
        for (OrderDetail od : order.getOrderDetails()) {
            OrderDetailResponse d = new OrderDetailResponse();
            d.setSkinId(od.getSkin() != null ? od.getSkin().getId() : null);
            d.setSkinName(od.getSkin() != null ? od.getSkin().getName() : null);
            d.setQuantity(od.getQuantity());
            d.setUnitPrice(od.getUnitPrice());
            details.add(d);
        }
        resp.setOrderDetailResponses(details);
        return resp;
    }
}
