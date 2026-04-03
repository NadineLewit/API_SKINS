package skinsmarket.demo.service;



import skinsmarket.demo.controller.order.OrderResponse;
import skinsmarket.demo.controller.order.OrderRequest;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.exception.NoStockAvailableException;
import skinsmarket.demo.exception.PropietarioSkinException;
import skinsmarket.demo.entity.User;

import java.util.List;

/**
 * Interfaz del servicio de Órdenes de compra.
 *
 */
public interface OrderService {

    /**
     * Crea una orden de compra para el usuario dado.
     * Valida stock, aplica descuento de cupón si hay, y descuenta el stock.
     * @throws NoStockAvailableException  si alguna skin no tiene stock suficiente
     * @throws PropietarioSkinException   si el usuario intenta comprar su propia skin
     */
    OrderResponse createOrder(User user, OrderRequest orderRequest)
            throws NoStockAvailableException, PropietarioSkinException;

    /** Devuelve el historial de órdenes del usuario, de más reciente a más antigua. */
    List<OrderResponse> getOrdersForUser(User user);

    /**
     * Obtiene una orden por ID, verificando que pertenezca al usuario dado.
     * Devuelve null si la orden no existe o no le pertenece.
     */
    OrderResponse getOrderById(Long id, String email);

    /** Devuelve todas las órdenes del sistema (para el panel de admin). */
    List<Order> findAllOrders();
}