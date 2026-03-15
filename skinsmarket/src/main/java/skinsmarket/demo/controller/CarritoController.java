package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.service.CarritoService;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService carritoService;

    // GET /carrito — ver mi carrito actual
    @GetMapping
    public ResponseEntity<Carrito> verCarrito(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(carritoService.obtenerOCrearCarrito(userDetails.getUsername()));
    }

    // PATCH /carrito/skins/{skinId}?cantidad=1 — agregar skin al carrito
    @PatchMapping("/skins/{skinId}")
    public ResponseEntity<Carrito> agregarSkin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long skinId,
            @RequestParam(defaultValue = "1") Integer cantidad) {
        return ResponseEntity.ok(carritoService.agregarSkin(userDetails.getUsername(), skinId, cantidad));
    }

    // PUT /carrito/items/{itemId}?cantidad=2 — modificar cantidad de un item
    @PutMapping("/items/{itemId}")
    public ResponseEntity<Carrito> modificarCantidad(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @RequestParam Integer cantidad) {
        return ResponseEntity.ok(carritoService.modificarCantidad(userDetails.getUsername(), itemId, cantidad));
    }

    // DELETE /carrito/items/{itemId} — quitar un item del carrito
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Carrito> eliminarItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.eliminarItem(userDetails.getUsername(), itemId));
    }

    // DELETE /carrito — vaciar todo el carrito
    @DeleteMapping
    public ResponseEntity<Carrito> vaciar(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(carritoService.vaciar(userDetails.getUsername()));
    }
}
