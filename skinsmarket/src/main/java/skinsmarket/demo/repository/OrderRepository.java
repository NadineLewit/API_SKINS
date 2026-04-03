package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Order.
 *
 * Idéntico al OrderRepository del TPO aprobado.
 * Gestiona la persistencia de las órdenes de compra de skins.
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save(), findById(), findAll(), deleteById(), etc.
 * El método findAll() es el que usa el AdminService para listar todas las órdenes.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Devuelve todas las órdenes de un usuario ordenadas por fecha descendente.
     *
     * Idéntico al TPO aprobado. Spring Data JPA deriva la query automáticamente:
     *   SELECT * FROM orders WHERE user_id = ? ORDER BY date DESC
     *
     * El ordenamiento DESC garantiza que el historial más reciente aparezca primero,
     * lo cual es la experiencia esperada al consultar GET /order/me.
     *
     * @param userId ID del usuario dueño de las órdenes
     * @return lista de órdenes del usuario, de más reciente a más antigua
     */
    List<Order> findByUserIdOrderByDateDesc(Long userId);
}
