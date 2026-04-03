package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Cupon.
 *
 * Nuevo repositorio respecto al TPO aprobado (los cupones de descuento
 * son una funcionalidad exclusiva del marketplace de skins).
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save()       → crear un nuevo cupón
 *   findById()   → buscar cupón por ID (para el panel de admin)
 *   findAll()    → listar todos los cupones
 *   deleteById() → eliminar un cupón
 */
@Repository
public interface CuponRepository extends JpaRepository<Cupon, Long> {

    /**
     * Busca un cupón por su código alfanumérico.
     *
     * Es la consulta principal del flujo de validación de cupones:
     * cuando un usuario ingresa un código en el checkout, el CuponService
     * llama a este método para verificar si el código existe.
     *
     * Se devuelve Optional para manejar limpiamente el caso en que el código
     * no exista, lanzando CuponInvalidoException en el servicio.
     *
     * @param codigo código del cupón a buscar (ej: "SUMMER20")
     * @return Optional con el cupón si el código existe, vacío si no
     */
    Optional<Cupon> findByCodigo(String codigo);
}
