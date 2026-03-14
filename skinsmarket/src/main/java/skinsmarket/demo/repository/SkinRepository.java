package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Skin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkinRepository extends JpaRepository<Skin, Long> {
    List<Skin> findByActivaTrue();
    List<Skin> findByCategoriaId(Long categoriaId);
    List<Skin> findByNombreContainingIgnoreCase(String nombre);
}