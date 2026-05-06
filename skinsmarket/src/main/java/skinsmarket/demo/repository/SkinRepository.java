package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;

import java.util.List;

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

    List<Skin> findByVendedor(User vendedor);

    List<Skin> findByNameContainingIgnoreCase(String name);

    /**
     * Filtra publicaciones por la categoría del catálogo asociado.
     * Ej: "Rifle", "Pistol", "Knife", "SMG", etc.
     * Case-insensitive y parcial (contains).
     */
    List<Skin> findByCatalogo_CategoryNameContainingIgnoreCase(String categoryName);

    List<Skin> findByPriceBetween(Double min, Double max);

    List<Skin> findByPriceLessThanEqual(Double max);

    List<Skin> findByPriceGreaterThanEqual(Double min);
}
