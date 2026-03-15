package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.Usuario;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    Optional<Carrito> findByUsuario(Usuario usuario);
}
