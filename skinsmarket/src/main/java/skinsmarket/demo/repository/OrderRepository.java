package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.OperationType;
import skinsmarket.demo.entity.TradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Order.
 *
 * Métodos nuevos para soportar venta/intercambio/devolución y el scheduler
 * del mock del bot.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Histórico de órdenes del usuario (más reciente primero). */
    List<Order> findByUserIdOrderByDateDesc(Long userId);

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
