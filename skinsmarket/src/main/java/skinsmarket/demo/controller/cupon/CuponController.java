package skinsmarket.demo.controller.cupon;

import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.exception.CuponInvalidoException;
import skinsmarket.demo.service.CuponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de Cupones de descuento.
 *
 * Sigue la misma estructura de controllers del TPO aprobado.
 *
 * Los cupones permiten aplicar descuentos al finalizar una compra.
 * Rutas de admin:   POST /cupones         (crear cupón)
 *                   GET  /cupones         (listar todos los cupones)
 * Rutas de usuario: POST /cupones/validar (validar un cupón antes de usarlo)
 */
@RestController
@RequestMapping("cupones")
public class CuponController {

    // Inyección del servicio de cupones (consistente con el estilo del TPO aprobado)
    @Autowired
    private CuponService cuponService;

    /**
     * Valida si un cupón es válido (activo y no expirado).
     * GET /cupones/validar?codigo=SUMMER20
     *
     * Se usa GET porque esta operación solo consulta el estado del cupón,
     * no lo modifica. El cupón se descuenta/desactiva recién al confirmar la orden.
     * Accesible por USER y ADMIN (el usuario lo usa en el checkout).
     *
     * @param codigo  código alfanumérico del cupón a validar
     * @throws CuponInvalidoException si el código no existe, está expirado o inactivo
     */
    @GetMapping("/validar")
    public ResponseEntity<Cupon> validarCupon(
            @RequestParam String codigo) throws CuponInvalidoException {
        return ResponseEntity.ok(cuponService.validar(codigo));
    }

    /**
     * Crea un nuevo cupón de descuento.
     * POST /cupones
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PostMapping
    public ResponseEntity<Cupon> crearCupon(@RequestBody CuponRequest cuponRequest) {
        Cupon result = cuponService.crear(cuponRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Devuelve todos los cupones registrados en el sistema.
     * GET /cupones
     * Solo accesible por usuarios con rol ADMIN.
     */
    @GetMapping
    public ResponseEntity<List<Cupon>> listarCupones() {
        List<Cupon> cupones = cuponService.listar();
        return ResponseEntity.ok(cupones);
    }

    /**
     * Obtiene un cupón específico por su ID.
     * GET /cupones/{id}
     * Solo accesible por usuarios con rol ADMIN.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Cupon> obtenerCupon(@PathVariable Long id) {
        Cupon cupon = cuponService.obtenerPorId(id);
        if (cupon != null) {
            return ResponseEntity.ok(cupon);
        } else {
            // Si no se encuentra el cupón, devuelve 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina un cupón por su ID.
     * DELETE /cupones/{id}
     * Solo accesible por usuarios con rol ADMIN.
     * Devuelve 204 No Content al eliminar correctamente.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCupon(@PathVariable Long id) {
        cuponService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}