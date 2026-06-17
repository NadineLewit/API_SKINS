package skinsmarket.demo.repository;

import skinsmarket.demo.entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad User.
 *
 * Spring Data JPA genera automáticamente la implementación en tiempo de ejecución.
 *
 * Hereda de JpaRepository los métodos estándar:
 *   save(), findById(), findAll(), deleteById(), existsById(), etc.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su email (usado como username en Spring Security y JWT).
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por su nombre de usuario.
     * Usado en el registro para verificar que el username no esté ya en uso.
     */
    Optional<User> findByUsername(String username);
}