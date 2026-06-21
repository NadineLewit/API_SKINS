package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioItemRepository extends JpaRepository<InventarioItem, Long> {

    /** Devuelve todos los items del inventario de un usuario. */
    @EntityGraph(attributePaths = "catalogo")
    List<InventarioItem> findByUser(User user);

    /**
     * Busca un item específico de un usuario por su assetId de Steam.
     * Se usa al sincronizar para detectar items existentes y no duplicarlos.
     */
    Optional<InventarioItem> findByUserAndAssetId(User user, String assetId);

    /** Borra todos los items de un user (útil para resync limpio). */
    void deleteByUser(User user);

    /** Busca un item pendiente creado por una reserva pagada. */
    Optional<InventarioItem> findByUserAndPendingOrderIdAndPendingSkinId(
            User user, Long pendingOrderId, Long pendingSkinId);

    /** Items internos creados para una orden de compra pendiente/entregada. */
    List<InventarioItem> findByPendingOrderId(Long pendingOrderId);
}
