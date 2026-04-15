package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Skin.
 *
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save(), findById(), findAll(), deleteById(), existsById(), etc.
 */
@Repository
public interface SkinRepository extends JpaRepository<Skin, Long> {

    /**
     * Devuelve todas las skins con stock mayor al valor indicado.
     *
     * Usado en getAllAvailableSkins() para devolver solo skins comprables.
     *
     * @param stock valor mínimo de stock (normalmente se pasa 0)
     * @return lista de skins con stock > stock
     */
    List<Skin> findByStockGreaterThan(int stock);

    /**
     * Devuelve todas las skins activas (no dadas de baja lógicamente).
     *
     * Usado en el catálogo público para mostrar solo skins disponibles.
     * active = true significa que la skin no fue eliminada por el admin/vendedor.
     *
     * @return lista de skins con active = true
     */
    List<Skin> findByActiveTrue();

    /**
     * Devuelve todas las skins inactivas (dadas de baja lógica).
     * Usado en el panel de admin con ?includeInactive=true
     */
    List<Skin> findByActiveFalse();

    /**
     * Devuelve todas las skins de una categoría específica (búsqueda por nombre).
     *
     * Navega la relación Skin → Category usando el nombre de la categoría.
     *
     * @param name nombre de la categoría (ej: "Rifle", "Pistola")
     * @return lista de skins que pertenecen a esa categoría
     */
    List<Skin> findByCategory_Name(String name);

    /** Busca skins por ID de categoría. */
    List<Skin> findByCategory_Id(Integer id);

    /**
     * Devuelve skins cuyo precio esté dentro del rango [min, max].
     *
     * Usado en el filtro de precios del catálogo público.
     *
     * @param min precio mínimo del rango
     * @param max precio máximo del rango
     * @return lista de skins con precio entre min y max (inclusive)
     */
    List<Skin> findByPriceBetween(Double min, Double max);

    /**
     * Devuelve skins con precio menor o igual al valor indicado.
     *
     * Usado cuando el usuario solo especifica un precio máximo en el filtro.
     *
     * @param price precio máximo a filtrar
     * @return lista de skins con price <= price
     */
    List<Skin> findByPriceLessThanEqual(Double price);

    /**
     * Devuelve skins con precio mayor o igual al valor indicado.
     *
     * Usado cuando el usuario solo especifica un precio mínimo en el filtro.
     *
     * @param price precio mínimo a filtrar
     * @return lista de skins con price >= price
     */
    List<Skin> findByPriceGreaterThanEqual(Double price);

    /**
     * Busca skins cuyo nombre contenga el texto dado (case-insensitive).
     *
     * Permite búsquedas parciales: buscar "dragon" encontrará "Dragon Lore", "Dragon King", etc.
     *
     * @param name texto a buscar dentro del nombre de la skin
     * @return lista de skins cuyo nombre contiene el texto (sin importar mayúsculas)
     */
    List<Skin> findByNameContainingIgnoreCase(String name);

    /**
     * Devuelve todas las skins publicadas por un vendedor específico.
     *
     * Nuevo método respecto al GameRepository del TPO (el marketplace de skins
     * permite a usuarios publicar sus propias skins para vender).
     *
     * @param vendedor usuario que publicó las skins
     * @return lista de skins publicadas por ese vendedor
     */
    List<Skin> findByVendedor(User vendedor);

    /**
     * Devuelve las skins activas publicadas por un vendedor específico.
     *
     * Variante de findByVendedor que filtra además por active = true.
     * Usado en el endpoint GET /skins/mis-skins para mostrar solo las
     * publicaciones vigentes del usuario autenticado.
     *
     * @param vendedor usuario que publicó las skins
     * @return lista de skins activas publicadas por ese vendedor
     */
    List<Skin> findByVendedorAndActiveTrue(User vendedor);
}
