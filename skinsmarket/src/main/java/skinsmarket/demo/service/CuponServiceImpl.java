package skinsmarket.demo.service;

import skinsmarket.demo.controller.cupon.CuponRequest;
import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.exception.CuponInvalidoException;
import skinsmarket.demo.repository.CuponRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementación del servicio de Cupones de descuento.
 *
 * Nueva implementación respecto al TPO aprobado.
 * Usa @Autowired y @Transactional igual que los demás servicios del proyecto.
 *
 * La validación del cupón aplica tres reglas:
 *   1. El código debe existir en la base de datos
 *   2. El cupón debe estar activo (activo = true)
 *   3. Si tiene fecha de vencimiento, esta no debe haber pasado
 */
@Service
public class CuponServiceImpl implements CuponService {

    @Autowired
    private CuponRepository cuponRepository;

    /**
     * Valida que un cupón sea aplicable para una compra.
     *
     * Verifica en orden: existencia → estado activo → fecha de vencimiento.
     * Si alguna condición falla, lanza CuponInvalidoException (HTTP 400).
     *
     * @param codigo código alfanumérico del cupón (ej: "SUMMER20")
     * @throws CuponInvalidoException si el cupón no existe, está inactivo o expiró
     */
    @Override
    public Cupon validar(String codigo) throws CuponInvalidoException {
        // 1. Buscar el cupón por código; si no existe lanzar excepción
        Cupon cupon = cuponRepository.findByCodigo(codigo)
                .orElseThrow(CuponInvalidoException::new);

        // 2. Verificar que el cupón esté activo
        if (!cupon.getActivo()) {
            throw new CuponInvalidoException();
        }

        // 3. Verificar que no haya vencido (solo si tiene fecha de vencimiento)
        if (cupon.getFechaVencimiento() != null &&
                cupon.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new CuponInvalidoException();
        }

        return cupon;
    }

    /**
     * Crea un nuevo cupón de descuento a partir del request del admin.
     * Persiste el cupón con los datos del CuponRequest.
     */
    @Override
    @Transactional
    public Cupon crear(CuponRequest cuponRequest) {
        Cupon cupon = new Cupon();
        cupon.setCodigo(cuponRequest.getCodigo().toUpperCase()); // normalizar a mayúsculas
        cupon.setDescuento(cuponRequest.getDescuento());
        cupon.setActivo(true);
        cupon.setFechaVencimiento(
                cuponRequest.getFechaExpiracion() != null
                        ? cuponRequest.getFechaExpiracion().toLocalDate()
                        : null);
        cupon.setMultiUso(
                cuponRequest.getMultiUso() != null ? cuponRequest.getMultiUso() : false);
        return cuponRepository.save(cupon);
    }

    /**
     * Devuelve todos los cupones registrados en el sistema.
     * Usado por el panel de administración.
     */
    @Override
    public List<Cupon> listar() {
        return cuponRepository.findAll();
    }

    /**
     * Obtiene un cupón por su ID.
     * Lanza IllegalArgumentException si no existe.
     */
    @Override
    public Cupon obtenerPorId(Long id) {
        return cuponRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cupón no encontrado: " + id));
    }

    /**
     * Elimina un cupón por su ID.
     */
    @Override
    @Transactional
    public void eliminar(Long id) {
        cuponRepository.deleteById(id);
    }
}
