package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de Skins.
 *
 * Spring Data JPA construye las queries automáticamente desde los nombres
 * de los métodos. Por ejemplo: findByCatalogo_CategoryName se traduce en
 *   SELECT s FROM Skin s WHERE s.catalogo.categoryName = ?
 * y eso a su vez en un JOIN con la tabla skin_catalogo.
 */
@Repository
public interface SkinRepository extends JpaRepository<Skin, Long> {

    List<Skin> findByActiveTrue();

    Optional<Skin> findByIdAndActiveTrueAndStockGreaterThan(Long id, Integer stock);

    List<Skin> findByActiveTrueAndStockGreaterThan(Integer stock);

    List<Skin> findByCatalogo_IdInAndActiveTrueAndStockGreaterThan(
            List<Long> catalogoIds, Integer stock);

    List<Skin> findByVendedor(User vendedor);

    List<Skin> findByNameContainingIgnoreCase(String name);

    List<Skin> findByNameContainingIgnoreCaseAndActiveTrueAndStockGreaterThan(
            String name, Integer stock);

    /**
     * Filtra publicaciones por la categoría del catálogo asociado.
     * Ej: "Rifle", "Pistol", "Knife", "SMG", etc.
     * Case-insensitive y parcial (contains).
     */
    List<Skin> findByCatalogo_CategoryNameContainingIgnoreCase(String categoryName);

    List<Skin> findByCatalogo_CategoryNameContainingIgnoreCaseAndActiveTrueAndStockGreaterThan(
            String categoryName, Integer stock);

    List<Skin> findByPriceBetween(Double min, Double max);

    List<Skin> findByPriceBetweenAndActiveTrueAndStockGreaterThan(
            Double min, Double max, Integer stock);

    List<Skin> findByPriceLessThanEqual(Double max);

    List<Skin> findByPriceLessThanEqualAndActiveTrueAndStockGreaterThan(
            Double max, Integer stock);

    List<Skin> findByPriceGreaterThanEqual(Double min);

    List<Skin> findByPriceGreaterThanEqualAndActiveTrueAndStockGreaterThan(
            Double min, Integer stock);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE s.active = true
              AND s.stock > 0
              AND (:catalogoId IS NULL OR s.catalogo.id = :catalogoId)
              AND ((:exterior IS NULL AND s.exterior IS NULL) OR s.exterior = :exterior)
              AND s.stattrak = :stattrak
            """)
    List<Skin> findPublicacionesComparables(
            @Param("catalogoId") Long catalogoId,
            @Param("exterior") Skin.Exterior exterior,
            @Param("stattrak") Boolean stattrak);
}
