package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.dto.SkinResponse;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.service.ISkinService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/skins")
@RequiredArgsConstructor
public class SkinController {

    private final ISkinService skinService;

    @GetMapping
    public ResponseEntity<List<SkinResponse>> listar() {
        List<SkinResponse> skins = skinService.listarActivas().stream()
                .map(SkinResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(skins);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkinResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(SkinResponse.fromEntity(skinService.obtenerPorId(id)));
    }

    @PostMapping
    public ResponseEntity<SkinResponse> crear(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Skin skin) {
        return ResponseEntity.ok(SkinResponse.fromEntity(skinService.crear(skin, userDetails.getUsername())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkinResponse> actualizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Skin skin) {
        return ResponseEntity.ok(SkinResponse.fromEntity(skinService.actualizar(id, skin, userDetails.getUsername())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        skinService.desactivar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mis-ventas")
    public ResponseEntity<List<SkinResponse>> misVentas(@AuthenticationPrincipal UserDetails userDetails) {
        List<SkinResponse> skins = skinService.misVentas(userDetails.getUsername()).stream()
                .map(SkinResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(skins);
    }

    @GetMapping("/mis-ventas/activas")
    public ResponseEntity<List<SkinResponse>> misVentasActivas(@AuthenticationPrincipal UserDetails userDetails) {
        List<SkinResponse> skins = skinService.misVentasActivas(userDetails.getUsername()).stream()
                .map(SkinResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(skins);
    }
}
