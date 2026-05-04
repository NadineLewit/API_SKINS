package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioItemRepository extends JpaRepository<InventarioItem, Long> {

    /** Devuelve todos los items del inventario de un usuario. */
    List<InventarioItem> findByUser(User user);

    /**
     * Busca un item específico de un usuario por su assetId de Steam.
     * Se usa al sincronizar para detectar items existentes y no duplicarlos.
     */
    Optional<InventarioItem> findByUserAndAssetId(User user, String assetId);

    /** Borra todos los items de un user (útil para resync limpio). */
    void deleteByUser(User user);
}
