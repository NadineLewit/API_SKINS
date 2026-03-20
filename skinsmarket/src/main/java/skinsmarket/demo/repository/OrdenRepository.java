package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skinsmarket.demo.entity.Orden;
import skinsmarket.demo.entity.Usuario;
import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    List<Orden> findByUsuario(Usuario usuario);
    List<Orden> findByDetalles_Skin_Vendedor(Usuario vendedor);
}