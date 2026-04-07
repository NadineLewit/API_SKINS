package skinsmarket.demo.controller.skin;

import java.util.List;

import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.service.SkinService;

/**
 * Controlador REST para Skins.
 *
 * CAMBIO (pedido por la profe):
 *   - Los endpoints de imagen ahora reciben MultipartFile y almacenan
 *     los bytes como BLOB en la BD (antes guardaban el archivo en disco).
 *   - La respuesta incluye imageBase64 que el frontend usa directamente.
 *
 * Uso en frontend:
 *   <img src={`data:image/jpeg;base64,${skin.imageBase64}`} />
 */
@RestController
@RequestMapping("skins")
public class SkinController {

    @Autowired
    private SkinService skinService;

    // =========================================================================
    // ENDPOINTS DE ADMINISTRADOR
    // =========================================================================

    /**
     * Crea una skin sin imagen desde el panel de admin.
     * POST /skins/admin/create
     * Body: JSON con los datos de la skin.
     */
    @PostMapping("/admin/create")
    public ResponseEntity<Skin> createSkin(@RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        Skin result = skinService.createSkin(skinRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Crea una skin con imagen desde el panel de admin.
     * POST /skins/admin/create-with-image
     *
     * Recibe multipart/form-data:
     *   - "skin": JSON con los datos (como RequestPart)
     *   - "image": archivo de imagen (como MultipartFile)
     *
     * La imagen se guarda como BLOB en la BD y se devuelve en base64.
     *
     * Ejemplo HTML (para el front):
     *   <form enctype="multipart/form-data">
     *     <input type="file" name="image" />
     *   </form>
     */
    @PostMapping(value = "/admin/create-with-image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createSkinWithImage(
            @RequestPart("skin") SkinRequest skinRequest,
            @RequestPart(value = "image", required = false) MultipartFile image)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        try {
            // Convertir MultipartFile a byte[] para almacenar en la BD como BLOB
            byte[] imageBytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;
            Skin result = skinService.createSkinWithImage(skinRequest, imageBytes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al crear la skin: " + e.getMessage());
        }
    }

    /**
     * Edita una skin existente sin imagen.
     * PUT /skins/admin/edit/{id}
     */
    @PutMapping("/admin/edit/{id}")
    public ResponseEntity<Skin> editSkin(@PathVariable Long id,
                                         @RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        Skin updated = skinService.editSkin(id, skinRequest);
        if (updated != null) return ResponseEntity.ok(updated);
        return ResponseEntity.notFound().build();
    }

    /**
     * Edita una skin con imagen nueva.
     * PUT /skins/admin/edit/{id}/with-image
     *
     * Recibe multipart/form-data:
     *   - "skin": JSON con los datos actualizados
     *   - "image": nuevo archivo de imagen
     */
    @PutMapping(value = "/admin/edit/{id}/with-image",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editSkinWithImage(
            @PathVariable Long id,
            @RequestPart("skin") SkinRequest skinRequest,
            @RequestPart(value = "image", required = false) MultipartFile image)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        try {
            byte[] imageBytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;
            Skin updated = skinService.editSkinWithImage(id, skinRequest, imageBytes);
            if (updated != null) return ResponseEntity.ok(updated);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al editar la skin: " + e.getMessage());
        }
    }

    /**
     * Baja lógica de una skin (active=false).
     * DELETE /skins/admin/delete/{id}
     */
    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> deleteSkin(@PathVariable Long id) {
        boolean deleted = skinService.deleteSkin(id);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    /**
     * Lista todas las skins (con o sin inactivas) para el panel admin.
     * GET /skins/admin/all?includeInactive=false
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<Skin>> getAllSkins(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(skinService.getAllSkins(includeInactive));
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    /** GET /skins/get/{id} — público */
    @GetMapping("/get/{id}")
    public ResponseEntity<Skin> getSkinById(@PathVariable Long id) {
        Skin skin = skinService.getSkinById(id);
        if (skin != null) return ResponseEntity.ok(skin);
        return ResponseEntity.notFound().build();
    }

    /** GET /skins/get/all — catálogo público (activas con stock) */
    @GetMapping("/get/all")
    public ResponseEntity<List<Skin>> getAllAvailableSkins() {
        return ResponseEntity.ok(skinService.getAllAvailableSkins());
    }

    /**
     * GET /skins/get/category?id=1
     * GET /skins/get/category?name=Rifle
     */
    @GetMapping("/get/category")
    public ResponseEntity<List<Skin>> getSkinsByCategory(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name) {
        if (id != null)                    return ResponseEntity.ok(skinService.getSkinsByCategoryId(id));
        if (name != null && !name.isBlank()) return ResponseEntity.ok(skinService.getSkinsByCategory(name));
        return ResponseEntity.badRequest().build();
    }

    /** GET /skins/get/price?min=10&max=100 */
    @GetMapping("/get/price")
    public ResponseEntity<List<Skin>> getSkinsByPrice(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        if (min != null && max != null) return ResponseEntity.ok(skinService.findByRangePrice(min, max));
        if (max != null)                return ResponseEntity.ok(skinService.findByPriceMax(max));
        if (min != null)                return ResponseEntity.ok(skinService.findByPriceMin(min));
        return ResponseEntity.badRequest().build();
    }

    /** GET /skins/get/search?name=AK */
    @GetMapping("/get/search")
    public ResponseEntity<List<Skin>> getSkinsByName(@RequestParam String name) {
        return ResponseEntity.ok(skinService.findByName(name));
    }

    // =========================================================================
    // ENDPOINTS DE VENDEDOR (USER autenticado)
    // =========================================================================

    /**
     * Publica una nueva skin. El vendedor se asigna del token JWT.
     * POST /skins
     */
    @PostMapping
    public ResponseEntity<Skin> publicarSkin(
            Authentication auth,
            @RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        Skin result = skinService.createSkinAsVendedor(skinRequest, auth.getName());
        return ResponseEntity.ok(result);
    }

    /** GET /skins/mis-skins — skins propias del usuario autenticado */
    @GetMapping("/mis-skins")
    public ResponseEntity<List<Skin>> misSkins(Authentication auth) {
        return ResponseEntity.ok(skinService.getSkinsByOwner(auth.getName()));
    }

    /**
     * Edita una skin propia. Solo el vendedor o un ADMIN puede hacerlo.
     * PUT /skins/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Skin> editarMiSkin(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        try {
            Skin updated = skinService.editSkinAsVendedor(id, skinRequest, auth.getName());
            if (updated != null) return ResponseEntity.ok(updated);
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    /**
     * Baja lógica de una skin propia.
     * DELETE /skins/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMiSkin(Authentication auth, @PathVariable Long id) {
        try {
            boolean deleted = skinService.deleteSkinAsVendedor(id, auth.getName());
            if (!deleted) return ResponseEntity.notFound().build();
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
