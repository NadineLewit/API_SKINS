package skinsmarket.demo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Skin s WHERE s.id = :id")
    Optional<Skin> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE s.id = :id
              AND s.active = true
              AND s.stock > :stock
              AND (s.estadoPublicacion IS NULL OR s.estadoPublicacion = skinsmarket.demo.entity.Skin.EstadoPublicacion.PUBLICADA)
            """)
    Optional<Skin> findPublicadaDisponibleById(@Param("id") Long id, @Param("stock") Integer stock);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE s.active = true
              AND s.stock > :stock
              AND (s.estadoPublicacion IS NULL OR s.estadoPublicacion = skinsmarket.demo.entity.Skin.EstadoPublicacion.PUBLICADA)
            """)
    List<Skin> findPublicadasDisponibles(@Param("stock") Integer stock);

    List<Skin> findByCatalogo_IdInAndActiveTrueAndStockGreaterThan(
            List<Long> catalogoIds, Integer stock);

    List<Skin> findByVendedor(User vendedor);

    List<Skin> findByNameContainingIgnoreCase(String name);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))
              AND s.active = true
              AND s.stock > :stock
              AND (s.estadoPublicacion IS NULL OR s.estadoPublicacion = skinsmarket.demo.entity.Skin.EstadoPublicacion.PUBLICADA)
            """)
    List<Skin> findPublicadasDisponiblesByName(
            @Param("name") String name, @Param("stock") Integer stock);

    /**
     * Filtra publicaciones por la categoría del catálogo asociado.
     * Ej: "Rifle", "Pistol", "Knife", "SMG", etc.
     * Case-insensitive y parcial (contains).
     */
    List<Skin> findByCatalogo_CategoryNameContainingIgnoreCase(String categoryName);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE LOWER(s.catalogo.categoryName) LIKE LOWER(CONCAT('%', :categoryName, '%'))
              AND s.active = true
              AND s.stock > :stock
              AND (s.estadoPublicacion IS NULL OR s.estadoPublicacion = skinsmarket.demo.entity.Skin.EstadoPublicacion.PUBLICADA)
            """)
    List<Skin> findPublicadasDisponiblesByCategoryName(
            @Param("categoryName") String categoryName, @Param("stock") Integer stock);

    List<Skin> findByPriceBetween(Double min, Double max);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE s.price BETWEEN :min AND :max
              AND s.active = true
              AND s.stock > :stock
              AND (s.estadoPublicacion IS NULL OR s.estadoPublicacion = skinsmarket.demo.entity.Skin.EstadoPublicacion.PUBLICADA)
            """)
    List<Skin> findPublicadasDisponiblesByPriceBetween(
            @Param("min") Double min, @Param("max") Double max, @Param("stock") Integer stock);

    List<Skin> findByPriceLessThanEqual(Double max);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE s.price <= :max
              AND s.active = true
              AND s.stock > :stock
              AND (s.estadoPublicacion IS NULL OR s.estadoPublicacion = skinsmarket.demo.entity.Skin.EstadoPublicacion.PUBLICADA)
            """)
    List<Skin> findPublicadasDisponiblesByPriceLessThanEqual(
            @Param("max") Double max, @Param("stock") Integer stock);

    List<Skin> findByPriceGreaterThanEqual(Double min);

    @Query("""
            SELECT s
            FROM Skin s
            WHERE s.price >= :min
              AND s.active = true
              AND s.stock > :stock
              AND (s.estadoPublicacion IS NULL OR s.estadoPublicacion = skinsmarket.demo.entity.Skin.EstadoPublicacion.PUBLICADA)
            """)
    List<Skin> findPublicadasDisponiblesByPriceGreaterThanEqual(
            @Param("min") Double min, @Param("stock") Integer stock);

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
