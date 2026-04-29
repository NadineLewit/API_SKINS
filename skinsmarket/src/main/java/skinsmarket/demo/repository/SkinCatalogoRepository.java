package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skinsmarket.demo.entity.SkinCatalogo;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkinCatalogoRepository extends JpaRepository<SkinCatalogo, Long> {

    /**
     * Busca por el ID externo de la API (ej: "skin-65604").
     * Se usa al sincronizar para no duplicar entradas existentes.
     */
    Optional<SkinCatalogo> findByExternalId(String externalId);

    /** Búsqueda por nombre parcial, case-insensitive (para el endpoint de search). */
    List<SkinCatalogo> findByNameContainingIgnoreCase(String name);

    /** Filtrado por arma (ej: "AK-47"). */
    List<SkinCatalogo> findByWeaponNameIgnoreCase(String weaponName);

    /** Filtrado por categoría (ej: "Rifles", "Pistols"). */
    List<SkinCatalogo> findByCategoryNameIgnoreCase(String categoryName);
}
