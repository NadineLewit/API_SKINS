package skinsmarket.demo.service;

import skinsmarket.demo.entity.Cupon;
import java.util.List;

public interface ICuponService {
    Cupon validar(String codigo);
    Cupon crear(Cupon cupon);
    List<Cupon> listar();
}
