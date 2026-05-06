package skinsmarket.demo.controller.skin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;
import skinsmarket.demo.service.SkinService;

/**
 * Controlador REST para Skins (publicaciones de venta).
 *
 * SIMPLIFICACIONES:
 *   - Sin multipart (las imágenes son URLs públicas del catálogo).
 *   - Sin entidad Category: el filtro por categoría usa el campo
 *     categoryName del catálogo asociado a cada skin.
 *
 * REGLA: USER no puede publicar libre, solo desde su inventario real
 * (POST /inventario/{id}/publicar). ADMIN sí puede crear/editar libre.
 */
@RestController
@RequestMapping("skins")
public class SkinController {

    @Autowired
    private SkinService skinService;

    // =========================================================================
    // ADMIN
    // =========================================================================

    @PostMapping("/admin/create")
    public ResponseEntity<?> createSkin(@RequestBody SkinRequest request) {
        try {
            Skin result = skinService.createSkin(request);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Skin creada exitosamente", result));
        } catch (NegativeStockException | NegativePriceException | InvalidDiscountException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of("Error al crear la skin: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/edit/{id}")
    public ResponseEntity<?> editSkin(@PathVariable Long id, @RequestBody SkinRequest request) {
        try {
            Skin updated = skinService.editSkin(id, request);
            if (updated != null) {
                return ResponseEntity.ok(ApiResponse.of("Skin actualizada exitosamente", updated));
            }
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Skin no encontrada con id: " + id));
        } catch (NegativeStockException | NegativePriceException | InvalidDiscountException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    @PutMapping("/admin/inactivar/{id}")
    public ResponseEntity<?> inactivarSkin(@PathVariable Long id) {
        boolean deleted = skinService.deleteSkin(id);
        if (!deleted) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Skin no encontrada con id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.of("Skin inactivada exitosamente"));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllSkins(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<Skin> skins = skinService.getAllSkins(includeInactive);
        return ResponseEntity.ok(
                ApiResponse.of("Listado de skins (" + skins.size() + ")", skins));
    }

    // =========================================================================
    // PÚBLICOS
    // =========================================================================

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSkinById(@PathVariable Long id) {
        try {
            Skin skin = skinService.getSkinById(id);
            return ResponseEntity.ok(ApiResponse.of("Skin encontrada", skin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Skin no encontrada con id: " + id));
        }
    }

    @GetMapping("/get/all")
    public ResponseEntity<?> getAllAvailableSkins() {
        List<Skin> skins = skinService.getAllAvailableSkins();
        return ResponseEntity.ok(
                ApiResponse.of("Catálogo de skins disponibles (" + skins.size() + ")", skins));
    }

    /**
     * GET /skins/get/category?name=Rifle
     *
     * Filtra publicaciones por categoría. La categoría sale del catálogo
     * de ByMykel (catalogo.categoryName). Valores típicos: "Rifle", "Pistol",
     * "Knife", "SMG", "Shotgun", "Sniper Rifle", "Machinegun", "Sticker", etc.
     */
    @GetMapping("/get/category")
    public ResponseEntity<?> getSkinsByCategory(@RequestParam String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of("Debés especificar 'name' como query param"));
        }
        List<Skin> skins = skinService.getSkinsByCategoryName(name);
        return ResponseEntity.ok(ApiResponse.of(
                "Skins de la categoría '" + name + "' (" + skins.size() + ")", skins));
    }

    @GetMapping("/get/price")
    public ResponseEntity<?> getSkinsByPrice(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        if (min != null && max != null) {
            return ResponseEntity.ok(ApiResponse.of(
                    "Skins entre " + min + " y " + max,
                    skinService.findByRangePrice(min, max)));
        }
        if (max != null) {
            return ResponseEntity.ok(ApiResponse.of(
                    "Skins con precio menor o igual a " + max,
                    skinService.findByPriceMax(max)));
        }
        if (min != null) {
            return ResponseEntity.ok(ApiResponse.of(
                    "Skins con precio mayor o igual a " + min,
                    skinService.findByPriceMin(min)));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.of("Debés especificar 'min' y/o 'max' como query param"));
    }

    @GetMapping("/get/search")
    public ResponseEntity<?> getSkinsByName(@RequestParam String name) {
        return ResponseEntity.ok(ApiResponse.of(
                "Resultados de búsqueda para: " + name,
                skinService.findByName(name)));
    }

    // =========================================================================
    // VENDEDOR (USER)
    // =========================================================================

    @GetMapping("/mis-skins")
    public ResponseEntity<?> misSkins(Authentication auth) {
        List<Skin> skins = skinService.getSkinsByOwner(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.of("Tus skins publicadas (" + skins.size() + ")", skins));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarMiSkin(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SkinRequest request) {
        try {
            Skin updated = skinService.editSkinAsVendedor(id, request, auth.getName());
            if (updated != null) {
                return ResponseEntity.ok(ApiResponse.of("Skin actualizada exitosamente", updated));
            }
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Skin no encontrada con id: " + id));
        } catch (NegativeStockException | NegativePriceException | InvalidDiscountException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    @PutMapping("/{id}/inactivar")
    public ResponseEntity<?> inactivarMiSkin(Authentication auth, @PathVariable Long id) {
        try {
            boolean deleted = skinService.deleteSkinAsVendedor(id, auth.getName());
            if (!deleted) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.of("Skin no encontrada con id: " + id));
            }
            return ResponseEntity.ok(ApiResponse.of("Skin inactivada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.of("No tenés permiso para inactivar esta skin"));
        }
    }

    @PostMapping
    public ResponseEntity<?> publicarSkinDeshabilitado() {
        return ResponseEntity.status(403).body(ApiResponse.of(
                "Los usuarios solo pueden publicar skins de su inventario real de Steam. " +
                "Usá POST /inventario/{itemId}/publicar después de sincronizar tu inventario."
        ));
    }
}
