package skinsmarket.demo.repository;

import skinsmarket.demo.entity.ItemCarrito;
import skinsmarket.demo.entity.Skin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad ItemCarrito.
 *
 *
 * Hereda de JpaRepository todos los métodos necesarios para el CRUD de ítems:
 *   save()      → crear o actualizar un ítem (agregar skin, modificar cantidad)
 *   findById()  → buscar un ítem específico por ID (para modificar cantidad)
 *   delete()    → eliminar un ítem del carrito
 *
 * No requiere métodos de consulta adicionales ya que los ítems siempre se
 * acceden a través del Carrito (via Carrito.getItems()), no directamente.
 */
@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
    List<ItemCarrito> findBySkin(Skin skin);
}
