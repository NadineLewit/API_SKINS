package skinsmarket.demo.service;

import skinsmarket.demo.controller.order.OrderDetailRequest;
import skinsmarket.demo.controller.order.OrderDetailResponse;
import skinsmarket.demo.controller.order.OrderRequest;
import skinsmarket.demo.controller.order.OrderResponse;
import skinsmarket.demo.entity.*;
import skinsmarket.demo.exception.CuponInvalidoException;
import skinsmarket.demo.exception.NoStockAvailableException;
import skinsmarket.demo.exception.PropietarioSkinException;
import skinsmarket.demo.repository.CuponRepository;
import skinsmarket.demo.repository.OrderRepository;
import skinsmarket.demo.repository.SkinRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de Órdenes de compra.
 *
 * mismo patrón de construcción de la orden, mismo mapeo a DTO de respuesta,
 * misma gestión del stock. Se agregan:
 *   - Soporte de cupón de descuento (codigoCupon en OrderRequest)
 *   - skinId en lugar de gameId en los detalles
 *   - Campos descuentoAplicado y totalFinal en la respuesta
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private CuponRepository cuponRepository;

    /**
     * Devuelve todas las órdenes del sistema (para el panel de admin).
     * Equivalente al findAllOrders() del TPO aprobado.
     */
    @Override
    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Devuelve el historial de órdenes del usuario autenticado.
     *
     * Mismo patrón de mapeo que el TPO aprobado (for loop + setters manuales).
     * Adaptado: skinId en lugar de gameId, se añade descuentoAplicado y totalFinal.
     */
    @Override
    public List<OrderResponse> getOrdersForUser(User user) {
        List<Order> orders = orderRepository.findByUserIdOrderByDateDesc(user.getId());
        List<OrderResponse> responses = new ArrayList<>();

        for (Order order : orders) {
            responses.add(mapToOrderResponse(order));
        }
        return responses;
    }

    /**
     * Obtiene una orden por ID verificando que le pertenezca al usuario dado.
     * Devuelve null si no existe o si pertenece a otro usuario.
     */
    @Override
    public OrderResponse getOrderById(Long id, String email) {
        return orderRepository.findById(id)
                .filter(o -> o.getUser().getEmail().equals(email))
                .map(this::mapToOrderResponse)
                .orElse(null);
    }

    /**
     * Crea una orden de compra a partir de la lista de ítems del request.
     *
     * Flujo (igual al TPO aprobado + lógica de cupón):
     *   1. Validar stock de cada skin
     *   2. Descontar stock
     *   3. Calcular totalPrice (suma de precio final × cantidad)
     *   4. Aplicar descuento de cupón si se proporcionó
     *   5. Persistir la orden y devolver el DTO de respuesta
     *
     * @throws NoStockAvailableException si alguna skin no tiene stock suficiente
     */
    @Override
    @Transactional
    public OrderResponse createOrder(User user, OrderRequest orderRequest)
            throws NoStockAvailableException, PropietarioSkinException {

        LocalDateTime now = LocalDateTime.now();

        // Validar que la orden tenga al menos un ítem.
        // Sin esto se crearía una orden con totalPrice=0 y sin detalles.
        if (orderRequest.getItemList() == null || orderRequest.getItemList().isEmpty()) {
            throw new RuntimeException("La orden debe contener al menos un ítem");
        }

        Order order = new Order();
        order.setUser(user);
        order.setDate(now);

        List<OrderDetailResponse> detailResponses = new ArrayList<>();
        double totalPrice = 0.0;

        // Procesar cada ítem de la orden
        for (OrderDetailRequest item : orderRequest.getItemList()) {
            Long skinId = item.getSkinId();
            Skin skin = skinRepository.findById(skinId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Skin no encontrada: " + skinId));

            // Validar que la skin esté activa (no haya sido dada de baja)
            if (!skin.getActive()) {
                throw new RuntimeException(
                        "La skin '" + skin.getName() + "' ya no está disponible para la compra"
                );
            }

            // Validar que el usuario no esté comprando su propia skin
            if (skin.getVendedor() != null &&
                    skin.getVendedor().getEmail().equals(user.getEmail())) {
                throw new PropietarioSkinException();
            }

            // Validar stock disponible (igual que el TPO aprobado)
            if (item.getQuantity() > skin.getStock()) {
                throw new NoStockAvailableException();
            }

            // Descontar stock de la skin
            skin.setStock(skin.getStock() - item.getQuantity());
            skinRepository.save(skin);

            // Precio final con descuento de la skin aplicado
            double finalPrice = skin.getFinalPrice();

            // Crear y agregar el detalle de la orden
            OrderDetail detail = new OrderDetail();
            detail.setSkin(skin);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(finalPrice);
            order.addOrderDetail(detail);

            // Construir el DTO de detalle para la respuesta
            OrderDetailResponse detailResponse = new OrderDetailResponse();
            detailResponse.setSkinId(skinId);
            detailResponse.setSkinName(skin.getName());
            detailResponse.setQuantity(item.getQuantity());
            detailResponse.setUnitPrice(finalPrice);
            detailResponses.add(detailResponse);

            totalPrice += item.getQuantity() * finalPrice;
        }

        // Aplicar cupón de descuento si se proporcionó
        double descuentoAplicado = 0.0;
        if (orderRequest.getCodigoCupon() != null &&
                !orderRequest.getCodigoCupon().isBlank()) {

            Cupon cupon = cuponRepository
                    .findByCodigo(orderRequest.getCodigoCupon())
                    .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

            // Validar cupón: activo y no vencido
            if (!cupon.getActivo()) {
                throw new RuntimeException("El cupón no está activo");
            }
            if (cupon.getFechaVencimiento() != null &&
                    cupon.getFechaVencimiento().isBefore(LocalDate.now())) {
                throw new RuntimeException("El cupón está vencido");
            }

            descuentoAplicado = cupon.getDescuento();

            // Si el cupón es de uso único, desactivarlo tras aplicarlo
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

        // Construir y devolver el DTO de respuesta completo
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

    // -------------------------------------------------------------------------
    // Método auxiliar de mapeo entidad → DTO
    // -------------------------------------------------------------------------

    /**
     * Convierte una entidad Order a su DTO de respuesta OrderResponse.
     * Extraído como método privado para reutilizarlo en getOrdersForUser y getOrderById.
     */
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