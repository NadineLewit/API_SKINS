package skinsmarket.demo.controller.carrito;

import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.service.CarritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión del Carrito de compras.
 *
 * El carrito permite al usuario agregar skins, modificar cantidades y vaciar el carrito
 * antes de finalizar la compra.
 *
 * Todas las rutas requieren autenticación (rol USER).
 * Ruta base: /carrito
 */
@RestController
@RequestMapping("carrito")
public class CarritoController {

    // Inyección del servicio de carrito
    @Autowired
    private CarritoService carritoService;

    /**
     * Devuelve el carrito actual del usuario autenticado.
     * Si el usuario no tiene carrito aún, lo crea automáticamente.
     * GET /carrito
     */
    @GetMapping
    public ResponseEntity<Carrito> verCarrito(Authentication auth) {
        // Obtenemos el email del usuario desde el token JWT
        String email = auth.getName();
        return ResponseEntity.ok(carritoService.obtenerOCrearCarrito(email));
    }

    /**
     * Agrega una skin al carrito del usuario autenticado.
     * Si la skin ya está en el carrito, incrementa la cantidad.
     * PATCH /carrito/skins/{skinId}?cantidad=1
     *
     * @param skinId   ID de la skin a agregar
     * @param cantidad cantidad a agregar (por defecto 1 si no se especifica)
     */
    @PatchMapping("/skins/{skinId}")
    public ResponseEntity<Carrito> agregarSkin(
            Authentication auth,
            @PathVariable Long skinId,
            @RequestParam(defaultValue = "1") Integer cantidad) {

        String email = auth.getName();
        return ResponseEntity.ok(carritoService.agregarSkin(email, skinId, cantidad));
    }

    /**
     * Modifica la cantidad de un ítem específico en el carrito.
     * PUT /carrito/items/{itemId}?cantidad=3
     *
     * @param itemId   ID del ítem del carrito a modificar
     * @param cantidad nueva cantidad (si es 0 o negativo, se elimina el ítem)
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<Carrito> modificarCantidad(
            Authentication auth,
            @PathVariable Long itemId,
            @RequestParam Integer cantidad) {

        String email = auth.getName();
        return ResponseEntity.ok(carritoService.modificarCantidad(email, itemId, cantidad));
    }

    /**
     * Elimina un ítem específico del carrito.
     * DELETE /carrito/items/{itemId}
     *
     * @param itemId ID del ítem del carrito a eliminar
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Carrito> eliminarItem(
            Authentication auth,
            @PathVariable Long itemId) {

        String email = auth.getName();
        return ResponseEntity.ok(carritoService.eliminarItem(email, itemId));
    }

    /**
     * Vacía completamente el carrito del usuario autenticado.
     * DELETE /carrito
     * Devuelve el carrito vacío como confirmación.
     */
    @DeleteMapping
    public ResponseEntity<Carrito> vaciarCarrito(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(carritoService.vaciar(email));
    }
}