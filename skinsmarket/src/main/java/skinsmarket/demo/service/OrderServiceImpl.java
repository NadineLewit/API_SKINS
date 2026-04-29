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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private CuponRepository cuponRepository;

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private UserRepository userRepository;

    // =========================================================================
    // Crear orden desde carrito (única forma de crear orden expuesta al cliente)
    // =========================================================================

    /**
     * Crea una orden de compra directamente desde el carrito del usuario.
     *
     * Flujo:
     *   1. Busca el usuario por email (del JWT)
     *   2. Busca su carrito y valida que no esté vacío
     *   3. Convierte los ítems del carrito a OrderDetailRequest
     *   4. Delega en createOrder() reutilizando toda la lógica existente
     *
     * Ventaja: el frontend no necesita mandar el body — solo el token y el cupón opcional.
     */
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

        // Convertir items del carrito a la estructura que espera createOrder()
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

        // Reutiliza toda la lógica de validación, descuento de stock y cupón
        return createOrder(user, orderRequest);
    }

    // =========================================================================
    // Crear orden con itemList explícito (interno — usado por createOrderFromCarrito)
    // =========================================================================

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

        for (OrderDetailRequest item : orderRequest.getItemList()) {
            Long skinId = item.getSkinId();
            Skin skin = skinRepository.findById(skinId)
                    .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + skinId));

            if (!skin.getActive()) {
                throw new RuntimeException("La skin '" + skin.getName() + "' ya no está disponible");
            }

            if (skin.getVendedor() != null &&
                    skin.getVendedor().getEmail().equals(user.getEmail())) {
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
        }

        // Aplicar cupón si se proporcionó
        double descuentoAplicado = 0.0;
        if (orderRequest.getCodigoCupon() != null && !orderRequest.getCodigoCupon().isBlank()) {
            Cupon cupon = cuponRepository.findByCodigo(orderRequest.getCodigoCupon())
                    .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

            if (!cupon.getActivo()) {
                throw new RuntimeException("El cupón no está activo");
            }
            if (cupon.getFechaVencimiento() != null &&
                    cupon.getFechaVencimiento().isBefore(LocalDate.now())) {
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

    // =========================================================================
    // Consultas
    // =========================================================================

    @Override
    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public List<OrderResponse> getOrdersForUser(User user) {
        List<Order> orders = orderRepository.findByUserIdOrderByDateDesc(user.getId());
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(mapToOrderResponse(order));
        }
        return responses;
    }

    @Override
    public OrderResponse getOrderById(Long id, String email) {
        return orderRepository.findById(id)
                .filter(o -> o.getUser().getEmail().equals(email))
                .map(this::mapToOrderResponse)
                .orElse(null);
    }

    // =========================================================================
    // Helper privado
    // =========================================================================

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
