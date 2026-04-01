package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.dto.OrdenResponse;
import skinsmarket.demo.service.IOrdenService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/ordenes")
@RequiredArgsConstructor
public class OrdenController {

    private final IOrdenService ordenService;

    @PostMapping
    public ResponseEntity<OrdenResponse> finalizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String cupon) {
        return ResponseEntity.ok(OrdenResponse.fromEntity(ordenService.finalizarCompra(userDetails.getUsername(), cupon)));
    }

    @GetMapping("/mias")
    public ResponseEntity<List<OrdenResponse>> misOrdenes(@AuthenticationPrincipal UserDetails userDetails) {
        List<OrdenResponse> ordenes = ordenService.misOrdenes(userDetails.getUsername()).stream()
                .map(OrdenResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ordenes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(OrdenResponse.fromEntity(ordenService.obtenerPorId(id)));
    }

    @GetMapping("/mis-ventas")
    public ResponseEntity<List<OrdenResponse>> misVentas(@AuthenticationPrincipal UserDetails userDetails) {
        List<OrdenResponse> ordenes = ordenService.misVentas(userDetails.getUsername()).stream()
                .map(OrdenResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ordenes);
    }
}
