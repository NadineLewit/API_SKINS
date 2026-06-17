package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Carrito.
 *

 *
 * A diferencia de la Wishlist (que solo necesitaba CRUD básico), el Carrito
 * requiere buscar por usuario para implementar la lógica de "obtener o crear carrito"
 * del CarritoService.
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save(), findById(), findAll(), deleteById(), etc.
 */
@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    /**
     * Busca el carrito asociado a un usuario.
     *
     * Cada usuario tiene exactamente un carrito (relación OneToOne en la entidad).
     * Se devuelve Optional para manejar el caso en que el usuario aún no tenga
     * carrito creado (en ese caso el CarritoService lo crea automáticamente).
     *
     * Usado en CarritoService.obtenerOCrearCarrito() para recuperar o inicializar
     * el carrito del usuario autenticado.
     *
     * @param user usuario dueño del carrito
     * @return Optional con el carrito si existe, vacío si el usuario aún no tiene uno
     */
    Optional<Carrito> findByUser(User user);
}
