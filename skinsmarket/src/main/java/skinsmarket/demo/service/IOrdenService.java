package skinsmarket.demo.service;

import skinsmarket.demo.entity.Orden;
import java.util.List;

public interface IOrdenService {
    Orden finalizarCompra(String username, String codigoCupon);
    List<Orden> misOrdenes(String username);
    Orden obtenerPorId(Long id);
    List<Orden> misVentas(String username);
}
