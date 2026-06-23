package skinsmarket.demo.service;

import skinsmarket.demo.controller.order.OrderResponse;
import skinsmarket.demo.controller.order.OrderRequest;
import skinsmarket.demo.controller.order.SellerSaleResponse;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.exception.NoStockAvailableException;
import skinsmarket.demo.exception.PropietarioSkinException;
import skinsmarket.demo.entity.User;
import java.util.List;

public interface OrderService {

    /**
     * Crea una orden a partir de un OrderRequest con itemList explícito.
     * Sigue siendo público en la interfaz porque createOrderFromCarrito lo reutiliza
     * internamente. NO está expuesto como endpoint REST.
     */
    OrderResponse createOrder(User user, OrderRequest orderRequest)
            throws NoStockAvailableException, PropietarioSkinException;

    /**
     * Crea una orden directamente desde el carrito del usuario.
     * Lee los ítems del carrito sin necesitar body en el request.
     *
     * @param email        email del usuario autenticado (del JWT)
     * @param codigoCupon  código de cupón opcional (null si no se usa)
     */
    OrderResponse createOrderFromCarrito(String email, String codigoCupon)
            throws NoStockAvailableException, PropietarioSkinException;

    /** Devuelve el historial de órdenes del usuario, de más reciente a más antigua. */
    List<OrderResponse> getOrdersForUser(User user);

    /** Ventas pagadas de las publicaciones del vendedor autenticado. */
    List<SellerSaleResponse> getPaidSalesForSeller(User seller);

    /** Obtiene una orden por ID verificando que pertenezca al usuario. Null si no. */
    OrderResponse getOrderById(Long id, String email);

    /** Cancela una compra pendiente y libera sus publicaciones reservadas. */
    OrderResponse cancelPendingPurchase(Long id, String email);

    /** Devuelve todas las órdenes del sistema (panel admin). */
    List<Order> findAllOrders();
}
