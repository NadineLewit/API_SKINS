package skinsmarket.demo.repository;

import jakarta.persistence.LockModeType;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.OperationType;
import skinsmarket.demo.entity.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Order.
 *
 * Métodos nuevos para soportar venta/intercambio/devolución y el scheduler
 * del mock del bot.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    /** Histórico de órdenes del usuario (más reciente primero). */
    List<Order> findByUserIdOrderByDateDesc(Long userId);

    /** Órdenes con sus detalles y skins inicializados para construir DTOs fuera de JPA. */
    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.skin " +
            "WHERE o.user.id = :userId ORDER BY o.date DESC")
    List<Order> findDetailedByUserIdOrderByDateDesc(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.orderDetails od " +
            "LEFT JOIN FETCH od.skin " +
            "WHERE o.id = :id")
    Optional<Order> findDetailedById(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN FETCH o.orderDetails od " +
            "JOIN FETCH od.skin s " +
            "WHERE s.vendedor.id = :sellerId " +
            "AND o.operationType = skinsmarket.demo.entity.OperationType.PURCHASE " +
            "AND o.paymentStatus = 'PAID' " +
            "ORDER BY o.date DESC")
    List<Order> findPaidPurchasesForSeller(@Param("sellerId") Long sellerId);

    /** Órdenes pendientes de pago de un usuario, de más reciente a más antigua. */
    List<Order> findByUserEmailAndPaymentStatusInOrderByDateDesc(
            String email,
            List<String> paymentStatuses
    );

    /** Verifica si una orden pertenece a un usuario. */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Devuelve todas las órdenes con un trade en alguno de los estados dados.
     * Usado por el scheduler de mock para encontrar órdenes a procesar.
     *
     * Ejemplo: findByTradeStatusIn([PREPARING_TRADE, BOT_SENT])
     */
    List<Order> findByTradeStatusIn(List<TradeStatus> statuses);

    /** Órdenes de un usuario filtradas por tipo de operación. */
    List<Order> findByUserIdAndOperationTypeOrderByDateDesc(Long userId, OperationType type);

    /** Órdenes de un usuario filtradas por estado del trade. */
    List<Order> findByUserIdAndTradeStatusOrderByDateDesc(Long userId, TradeStatus status);

    /** Compras pagadas en estados donde el inventario interno puede necesitar sincronizarse. */
    List<Order> findByOperationTypeAndPaymentStatusAndTradeStatusIn(
            OperationType operationType, String paymentStatus, List<TradeStatus> statuses);

    /**
     * Busca órdenes que tengan un assetId específico en su userSkinAssetIds.
     * Usado para evitar que la misma skin se use en dos operaciones simultáneas.
     * Como guardamos los assetIds como JSON string, usamos LIKE.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT o FROM Order o WHERE o.userSkinAssetIds LIKE %:assetId% " +
            "AND o.tradeStatus NOT IN (skinsmarket.demo.entity.TradeStatus.COMPLETED, " +
            "                          skinsmarket.demo.entity.TradeStatus.CANCELLED, " +
            "                          skinsmarket.demo.entity.TradeStatus.RETURNED, " +
            "                          skinsmarket.demo.entity.TradeStatus.RETURN_FAILED, " +
            "                          skinsmarket.demo.entity.TradeStatus.FAILED, " +
            "                          skinsmarket.demo.entity.TradeStatus.EXPIRED)")
    List<Order> findActiveOrdersWithAssetId(@org.springframework.data.repository.query.Param("assetId") String assetId);
}
