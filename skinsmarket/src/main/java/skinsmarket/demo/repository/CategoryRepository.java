package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Category.
 *
 * Idéntico al CategoryRepository del TPO aprobado.
 * Provee acceso a las categorías de skins almacenadas en la base de datos.
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save(), findById(), findAll(), deleteById(), etc.
 * Además incluye una consulta personalizada para búsqueda por nombre.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Busca todas las categorías que tengan exactamente ese nombre.
     * Usado para verificar duplicados exactos.
     */
    @Query(value = "select c from Category c where c.name = ?1")
    List<Category> findByName(String name);

    /**
     * Busca categorías por nombre ignorando mayúsculas/minúsculas.
     * Usado para prevenir duplicados case-insensitive: "Rifle" y "rifle"
     * no deben poder coexistir como categorías distintas.
     */
    @Query(value = "select c from Category c where lower(c.name) = lower(?1)")
    List<Category> findByNameIgnoreCase(String name);
}