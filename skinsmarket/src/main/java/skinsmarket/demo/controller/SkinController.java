package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.service.ISkinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/skins")
@RequiredArgsConstructor
public class SkinController {

    private final ISkinService skinService;

    // GET /skins — catálogo público, con filtros opcionales
    // Ejemplo: GET /skins?nombre=ak&rareza=ROJO&exterior=CASI_NUEVO&precioMin=10&precioMax=500&categoriaId=1
    @GetMapping
    public List<Skin> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Skin.Rareza rareza,
            @RequestParam(required = false) Skin.Exterior exterior,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false) Long categoriaId) {
        return skinService.listarConFiltros(nombre, rareza, exterior, precioMin, precioMax, categoriaId);
    }

    // GET /skins/{id} — detalle de una skin
    @GetMapping("/{id}")
    public ResponseEntity<Skin> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(skinService.obtenerPorId(id));
    }

    // POST /skins — publicar una skin (requiere token)
    @PostMapping
    public ResponseEntity<Skin> crear(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody Skin skin) {
        return ResponseEntity.ok(skinService.crear(skin, userDetails.getUsername()));
    }

    // PUT /skins/{id} — editar una skin propia
    @PutMapping("/{id}")
    public ResponseEntity<Skin> actualizar(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody Skin skin) {
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