package skinsmarket.demo.controller.carrito;

import skinsmarket.demo.controller.common.ApiResponse;
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
 * Todas las rutas requieren autenticación (rol USER o ADMIN).
 * Ruta base: /carrito
 *
 * Las cantidades se pasan como query param (?cantidad=X), NO en el body,
 * porque las modificaciones del carrito son parciales y no requieren payload JSON.
 */
@RestController
@RequestMapping("carrito")
public class CarritoController {

    @Autowired
    private CarritoService carritoService;

    /**
     * Devuelve el carrito actual del usuario autenticado.
     * Si el usuario no tiene carrito aún, lo crea automáticamente.
     * GET /carrito
     */
    @GetMapping
    public ResponseEntity<?> verCarrito(Authentication auth) {
        String email = auth.getName();
        Carrito carrito = carritoService.obtenerOCrearCarrito(email);
        return ResponseEntity.ok(ApiResponse.of("Carrito del usuario", carrito));
    }

    /**
     * Agrega una skin al carrito del usuario autenticado.
     * Si la skin ya está en el carrito, incrementa la cantidad.
     * PATCH /carrito/skins/{skinId}?cantidad=1
     */
    @PatchMapping("/skins/{skinId}")
    public ResponseEntity<?> agregarSkin(
            Authentication auth,
            @PathVariable Long skinId,
            @RequestParam(defaultValue = "1") Integer cantidad) {

        String email = auth.getName();
        Carrito carrito = carritoService.agregarSkin(email, skinId, cantidad);
        return ResponseEntity.ok(ApiResponse.of("Skin agregada al carrito", carrito));
    }

    /**
     * Modifica la cantidad de un ítem específico en el carrito.
     * PUT /carrito/items/{itemId}?cantidad=3
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> modificarCantidad(
            Authentication auth,
            @PathVariable Long itemId,
            @RequestParam Integer cantidad) {

        String email = auth.getName();
        Carrito carrito = carritoService.modificarCantidad(email, itemId, cantidad);
        return ResponseEntity.ok(ApiResponse.of("Cantidad del ítem actualizada", carrito));
    }

    /**
     * Elimina un ítem específico del carrito.
     * DELETE /carrito/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> eliminarItem(
            Authentication auth,
            @PathVariable Long itemId) {

        String email = auth.getName();
        Carrito carrito = carritoService.eliminarItem(email, itemId);
        return ResponseEntity.ok(ApiResponse.of("Ítem eliminado del carrito", carrito));
    }

    /**
     * Vacía completamente el carrito del usuario autenticado.
     * DELETE /carrito
     */
    @DeleteMapping
    public ResponseEntity<?> vaciarCarrito(Authentication auth) {
        String email = auth.getName();
        Carrito carrito = carritoService.vaciar(email);
        return ResponseEntity.ok(ApiResponse.of("Carrito vaciado correctamente", carrito));
    }
}
