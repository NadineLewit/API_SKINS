package skinsmarket.demo.repository;

import skinsmarket.demo.entity.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad ItemCarrito.
 *
 * Nuevo repositorio respecto al TPO aprobado (el carrito con ítems detallados
 * es una funcionalidad exclusiva del marketplace de skins).
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
    // Los métodos heredados de JpaRepository son suficientes para este repositorio.
    // La navegación por ítems se realiza a través de Carrito.getItems()
    // gracias a la relación OneToMany con CascadeType.ALL configurada en Carrito.
}
