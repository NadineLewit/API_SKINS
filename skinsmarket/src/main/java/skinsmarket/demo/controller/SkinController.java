package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.service.ISkinService;
import skinsmarket.demo.service.SkinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/skins")
@RequiredArgsConstructor
public class SkinController {

    private final ISkinService skinService;

    // GET /skins — catálogo público, todas las skins activas
    @GetMapping
    public List<Skin> listar() {
        return skinService.listarActivas();
    }

    // GET /skins/{id} — detalle de una skin
    @GetMapping("/{id}")
    public ResponseEntity<Skin> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(skinService.obtenerPorId(id));
    }

    // POST /skins — publicar una skin (cualquier usuario autenticado puede vender)
    @PostMapping
    public ResponseEntity<Skin> crear(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Skin skin) {
        return ResponseEntity.ok(skinService.crear(skin, userDetails.getUsername()));
    }

    // PUT /skins/{id} — editar una skin
    @PutMapping("/{id}")
    public ResponseEntity<Skin> actualizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Skin skin) {
        return ResponseEntity.ok(skinService.actualizar(id, skin, userDetails.getUsername()));
    }

    // DELETE /skins/{id} — desactivar una skin (baja lógica)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        skinService.desactivar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // GET /skins/mis-ventas — ver todas las skins que publiqué
    @GetMapping("/mis-ventas")
    public ResponseEntity<List<Skin>> misVentas(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(skinService.misVentas(userDetails.getUsername()));
    }

    // GET /skins/mis-ventas/activas — ver solo las skins activas que publiqué
    @GetMapping("/mis-ventas/activas")
    public ResponseEntity<List<Skin>> misVentasActivas(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(skinService.misVentasActivas(userDetails.getUsername()));
    }
}