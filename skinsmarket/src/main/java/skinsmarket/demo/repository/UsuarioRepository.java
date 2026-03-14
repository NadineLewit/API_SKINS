package skinsmarket.demo.repository;

import skinsmarket.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
}