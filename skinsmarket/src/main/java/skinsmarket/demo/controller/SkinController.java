package skinsmarket.demo.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import skinsmarket.demo.entity.Skin;
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

    private final SkinService skinService;

    @GetMapping
    public List<Skin> listar() {
        return skinService.listarActivas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skin> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(skinService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Skin> crear(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Skin skin) {
        return ResponseEntity.ok(skinService.crear(skin, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Skin> actualizar(@PathVariable Long id, @RequestBody Skin skin) {
        return ResponseEntity.ok(skinService.actualizar(id, skin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        skinService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}