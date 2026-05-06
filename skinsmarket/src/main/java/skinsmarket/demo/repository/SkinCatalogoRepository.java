package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skinsmarket.demo.entity.SkinCatalogo;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkinCatalogoRepository extends JpaRepository<SkinCatalogo, Long> {

    /** Busca por id externo de ByMykel. Usado en el sync para upsert. */
    Optional<SkinCatalogo> findByExternalId(String externalId);

    /** Match exacto por nombre completo. Usado en el matching de inventario. */
    Optional<SkinCatalogo> findByName(String name);

    /** Match parcial case-insensitive. Usado en /catalogo/buscar y como fallback. */
    List<SkinCatalogo> findByNameContainingIgnoreCase(String name);

    /** Filtros para /catalogo/filtrar — todos parciales y case-insensitive. */
    List<SkinCatalogo> findByWeaponNameContainingIgnoreCase(String weaponName);

    List<SkinCatalogo> findByCategoryNameContainingIgnoreCase(String categoryName);

    List<SkinCatalogo> findByWeaponNameContainingIgnoreCaseAndCategoryNameContainingIgnoreCase(
            String weaponName, String categoryName);
}
