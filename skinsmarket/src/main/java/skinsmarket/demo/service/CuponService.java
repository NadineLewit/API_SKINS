package skinsmarket.demo.service;

import skinsmarket.demo.controller.cupon.CuponRequest;
import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.exception.CuponInvalidoException;

import java.util.List;

/**
 * Interfaz del servicio de Cupones de descuento.
 *
 * Nueva interfaz respecto al TPO aprobado.
 * Sigue el mismo patrón interfaz + implementación del TPO.
 */
public interface CuponService {

    /**
     * Valida si un cupón es aplicable (activo y no vencido).
     * @throws CuponInvalidoException si el cupón no existe, está inactivo o vencido
     */
    Cupon validar(String codigo) throws CuponInvalidoException;

    /** Crea un nuevo cupón de descuento. */
    Cupon crear(CuponRequest cuponRequest);

    /** Devuelve todos los cupones registrados. */
    List<Cupon> listar();

    /** Obtiene un cupón por su ID. */
    Cupon obtenerPorId(Long id);

    /** Elimina un cupón por su ID. */
    void eliminar(Long id);
}
