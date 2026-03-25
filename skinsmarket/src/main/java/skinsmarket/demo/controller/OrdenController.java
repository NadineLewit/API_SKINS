package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.entity.Orden;
import skinsmarket.demo.service.IOrdenService;
import skinsmarket.demo.service.OrdenService;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final IOrdenService ordenService;

    @PostMapping
    public ResponseEntity<Orden> finalizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String cupon) {
        return ResponseEntity.ok(ordenService.finalizarCompra(userDetails.getUsername(), cupon));
    }

    @GetMapping("/mias")
    public ResponseEntity<List<Orden>> misOrdenes(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ordenService.misOrdenes(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Orden> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenService.obtenerPorId(id));
    }

    @GetMapping("/mis-ventas")
    public ResponseEntity<List<Orden>> misVentas(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ordenService.misVentas(userDetails.getUsername()));
    }
}