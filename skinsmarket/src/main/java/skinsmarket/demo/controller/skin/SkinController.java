package skinsmarket.demo.controller.skin;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * REGLA: Toda skin requiere imagen obligatoria.
 * Los endpoints de creación y edición usan multipart/form-data.
 *   - Parte "skin"  → JSON con los datos de la skin (como texto)
 *   - Parte "image" → archivo de imagen (MultipartFile)
 *
 * Frontend renderiza con: <img src={`data:image/jpeg;base64,${skin.imageBase64}`} />
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
     * Crea una skin con imagen obligatoria.
     * POST /skins/admin/create-with-image
     * Body: multipart/form-data
     *   - skin:  JSON string con los datos
     *   - image: archivo de imagen (obligatorio)
     */
    @PostMapping(value = "/admin/create-with-image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createSkinWithImage(
            @RequestPart("skin") String skinJson,
            @RequestPart("image") MultipartFile image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            SkinRequest skinRequest = mapper.readValue(skinJson, SkinRequest.class);
            byte[] imageBytes = image.getBytes();
            Skin result = skinService.createSkinWithImage(skinRequest, imageBytes);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al crear la skin: " + e.getMessage());
        }
    }

    /**
     * Edita una skin con imagen obligatoria.
     * PUT /skins/admin/edit/{id}/with-image
     * Body: multipart/form-data
     *   - skin:  JSON string con los datos actualizados
     *   - image: nuevo archivo de imagen (obligatorio)
     */
    @PutMapping(value = "/admin/edit/{id}/with-image",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editSkinWithImage(
            @PathVariable Long id,
            @RequestPart("skin") String skinJson,
            @RequestPart("image") MultipartFile image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            SkinRequest skinRequest = mapper.readValue(skinJson, SkinRequest.class);
            byte[] imageBytes = image.getBytes();
            Skin updated = skinService.editSkinWithImage(id, skinRequest, imageBytes);
            if (updated != null) return ResponseEntity.ok(updated);
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
     * Lista todas las skins para el panel admin.
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

    /** GET /skins/get/category?id=1  o  ?name=Rifle */
    @GetMapping("/get/category")
    public ResponseEntity<List<Skin>> getSkinsByCategory(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name) {
        if (id != null) return ResponseEntity.ok(skinService.getSkinsByCategoryId(id));
        if (name != null && !name.isBlank()) return ResponseEntity.ok(skinService.getSkinsByCategory(name));
        return ResponseEntity.badRequest().build();
    }

    /** GET /skins/get/price?min=10&max=100 */
    @GetMapping("/get/price")
    public ResponseEntity<List<Skin>> getSkinsByPrice(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        if (min != null && max != null) return ResponseEntity.ok(skinService.findByRangePrice(min, max));
        if (max != null) return ResponseEntity.ok(skinService.findByPriceMax(max));
        if (min != null) return ResponseEntity.ok(skinService.findByPriceMin(min));
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
     * Publica una skin con imagen obligatoria.
     * POST /skins/with-image
     * Body: multipart/form-data
     *   - skin:  JSON string con los datos
     *   - image: archivo de imagen (obligatorio)
     */
    @PostMapping(value = "/with-image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> publicarSkinConImagen(
            Authentication auth,
            @RequestPart("skin") String skinJson,
            @RequestPart("image") MultipartFile image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            SkinRequest skinRequest = mapper.readValue(skinJson, SkinRequest.class);
            byte[] imageBytes = image.getBytes();
            Skin result = skinService.createSkinAsVendedor(skinRequest, auth.getName(), imageBytes);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al publicar la skin: " + e.getMessage());
        }
    }

    /** GET /skins/mis-skins */
    @GetMapping("/mis-skins")
    public ResponseEntity<List<Skin>> misSkins(Authentication auth) {
        return ResponseEntity.ok(skinService.getSkinsByOwner(auth.getName()));
    }

    /**
     * Edita una skin propia con imagen obligatoria.
     * PUT /skins/{id}/with-image
     */
    @PutMapping(value = "/{id}/with-image",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editarMiSkinConImagen(
            Authentication auth,
            @PathVariable Long id,
            @RequestPart("skin") String skinJson,
            @RequestPart("image") MultipartFile image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            SkinRequest skinRequest = mapper.readValue(skinJson, SkinRequest.class);
            byte[] imageBytes = image.getBytes();
            Skin updated = skinService.editSkinAsVendedor(id, skinRequest, auth.getName(), imageBytes);
            if (updated != null) return ResponseEntity.ok(updated);
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al editar la skin: " + e.getMessage());
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
