package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.dto.CarritoResponse;
import skinsmarket.demo.service.ICarritoService;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/carrito")
@RequiredArgsConstructor
public class CarritoController {

    private final ICarritoService carritoService;

    @GetMapping
    public ResponseEntity<CarritoResponse> verCarrito(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(CarritoResponse.fromEntity(carritoService.obtenerOCrearCarrito(userDetails.getUsername())));
    }

    @PatchMapping("/skins/{skinId}")
    public ResponseEntity<CarritoResponse> agregarSkin(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long skinId,
            @RequestParam(defaultValue = "1") Integer cantidad) {
        return ResponseEntity.ok(CarritoResponse.fromEntity(carritoService.agregarSkin(userDetails.getUsername(), skinId, cantidad)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> modificarCantidad(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @RequestParam Integer cantidad) {
        return ResponseEntity.ok(CarritoResponse.fromEntity(carritoService.modificarCantidad(userDetails.getUsername(), itemId, cantidad)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> eliminarItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(CarritoResponse.fromEntity(carritoService.eliminarItem(userDetails.getUsername(), itemId)));
    }

    @DeleteMapping
    public ResponseEntity<CarritoResponse> vaciar(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(CarritoResponse.fromEntity(carritoService.vaciar(userDetails.getUsername())));
    }
}
