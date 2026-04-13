package skinsmarket.demo.controller.cupon;

import skinsmarket.demo.entity.Cupon;
import skinsmarket.demo.exception.CuponInvalidoException;
import skinsmarket.demo.service.CuponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("cupones")
public class CuponController {

    @Autowired
    private CuponService cuponService;

    // =========================================================================
    // Validar cupón
    // GET /cupones/validar?codigo=PROMO2027
    // TOKEN: USER o ADMIN
    // =========================================================================
    @GetMapping("/validar")
    public ResponseEntity<Cupon> validarCupon(
            @RequestParam String codigo) throws CuponInvalidoException {
        return ResponseEntity.ok(cuponService.validar(codigo));
    }

    // =========================================================================
    // Crear cupón
    // POST /cupones
    // TOKEN: ADMIN
    // =========================================================================
    @PostMapping
    public ResponseEntity<Cupon> crearCupon(@RequestBody CuponRequest cuponRequest) {
        return ResponseEntity.ok(cuponService.crear(cuponRequest));
    }

    // =========================================================================
    // Listar todos los cupones
    // GET /cupones
    // TOKEN: ADMIN
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<Cupon>> listarCupones() {
        return ResponseEntity.ok(cuponService.listar());
    }

    // =========================================================================
    // Obtener cupón por ID (nuevo)
    // GET /cupones/{id}
    // TOKEN: ADMIN
    // =========================================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCuponPorId(@PathVariable Long id) {
        try {
            Cupon cupon = cuponService.obtenerPorId(id);
            return ResponseEntity.ok(cupon);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body("Cupón no encontrado con id: " + id);
        }
    }

    // =========================================================================
    // Eliminar cupón
    // DELETE /cupones/{id}
    // TOKEN: ADMIN
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarCupon(@PathVariable Long id) {
        cuponService.eliminar(id);
        return ResponseEntity.ok("Cupón eliminado exitosamente");
    }
}
