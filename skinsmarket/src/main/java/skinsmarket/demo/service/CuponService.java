package skinsmarket.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.exception.CuponInvalidoException;
import skinsmarket.demo.repository.CuponRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CuponService {

    private final CuponRepository cuponRepository;

    public Cupon validar(String codigo) {
        Cupon cupon = cuponRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        if (!cupon.getActivo()) throw new CuponInvalidoException();
        if (cupon.getFechaVencimiento() != null &&
                cupon.getFechaVencimiento().isBefore(java.time.LocalDate.now())) {
            throw new CuponInvalidoException();
        }
        return cupon;
    }

    public Cupon crear(Cupon cupon) {
        return cuponRepository.save(cupon);
    }

    public List<Cupon> listar() {
        return cuponRepository.findAll();
    }
}