package skinsmarket.demo.controller.cupon;

import skinsmarket.demo.controller.common.ApiResponse;
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
 * Los cupones se aplican sobre el TOTAL de una orden y los crea el ADMIN.
 * Cada cupón tiene un código único, un % de descuento, fecha de vencimiento y
 * puede ser de un solo uso o multiuso.
 *
 * Todas las respuestas siguen el formato uniforme ApiResponse.
 */
@RestController
@RequestMapping("cupones")
public class CuponController {

    @Autowired
    private CuponService cuponService;

    /**
     * Valida un cupón antes de aplicarlo.
     * GET /cupones/validar?codigo=PROMO2027
     * TOKEN: USER o ADMIN
     */
    @GetMapping("/validar")
    public ResponseEntity<?> validarCupon(
            @RequestParam String codigo) throws CuponInvalidoException {
        Cupon cupon = cuponService.validar(codigo);
        return ResponseEntity.ok(ApiResponse.of("Cupón válido", cupon));
    }

    /**
     * Crea un nuevo cupón.
     * POST /cupones
     * TOKEN: ADMIN
     */
    @PostMapping
    public ResponseEntity<?> crearCupon(@RequestBody CuponRequest cuponRequest) {
        Cupon cupon = cuponService.crear(cuponRequest);
        return ResponseEntity.status(201)
                .body(ApiResponse.of("Cupón creado exitosamente", cupon));
    }

    /**
     * Lista todos los cupones del sistema.
     * GET /cupones
     * TOKEN: ADMIN
     */
    @GetMapping
    public ResponseEntity<?> listarCupones() {
        List<Cupon> cupones = cuponService.listar();
        return ResponseEntity.ok(
                ApiResponse.of("Listado de cupones (" + cupones.size() + ")", cupones));
    }

    /**
     * Obtiene un cupón por su ID.
     * GET /cupones/{id}
     * TOKEN: ADMIN
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCuponPorId(@PathVariable Long id) {
        try {
            Cupon cupon = cuponService.obtenerPorId(id);
            return ResponseEntity.ok(ApiResponse.of("Cupón encontrado", cupon));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Cupón no encontrado con id: " + id));
        }
    }

    /**
     * Elimina un cupón por su ID.
     * DELETE /cupones/{id}
     * TOKEN: ADMIN
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCupon(@PathVariable Long id) {
        cuponService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.of("Cupón eliminado exitosamente"));
    }
}
