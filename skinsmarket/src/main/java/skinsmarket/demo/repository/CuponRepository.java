package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skinsmarket.demo.entity.Cupon;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {
    Optional<Cupon> findByCodigo(String codigo);
}