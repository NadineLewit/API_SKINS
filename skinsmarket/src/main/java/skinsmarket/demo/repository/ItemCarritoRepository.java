package skinsmarket.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skinsmarket.demo.entity.ItemCarrito;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long> {
}
