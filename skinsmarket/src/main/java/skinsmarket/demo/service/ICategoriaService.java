package skinsmarket.demo.service;

import skinsmarket.demo.entity.Categoria;
import java.util.List;

public interface ICategoriaService {
    List<Categoria> listarTodas();
    Categoria obtenerPorId(Long id);
    Categoria crear(Categoria categoria);
    Categoria actualizar(Long id, Categoria datos);
    void eliminar(Long id);
}
