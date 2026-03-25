package skinsmarket.demo.service;

import skinsmarket.demo.entity.Skin;
import java.util.List;

public interface ISkinService {
    List<Skin> listarActivas();
    Skin obtenerPorId(Long id);
    Skin crear(Skin skin, String username);
    Skin actualizar(Long id, Skin datos, String username);
    void desactivar(Long id, String username);
    List<Skin> misVentas(String username);
    List<Skin> misVentasActivas(String username);
}
