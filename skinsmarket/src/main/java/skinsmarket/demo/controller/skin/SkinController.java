package skinsmarket.demo.controller.skin;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.service.SkinService;

/**
 * Controlador REST para Skins (publicaciones de venta).
 *
 * REGLA DE NEGOCIO PRINCIPAL:
 *   - Los USER (vendedores comunes) NO pueden publicar skins libres ni
 *     directamente desde el catálogo. La ÚNICA forma de publicar como USER
 *     es desde el inventario real de Steam: POST /inventario/{itemId}/publicar
 *     Esto evita que cualquiera pueda vender una skin que no tiene.
 *   - Los ADMIN sí pueden crear/editar libremente (para casos especiales o testing).
 *
 * BAJA LÓGICA: las skins NO se eliminan físicamente porque tienen FK desde
 * order_details. En su lugar se inactivan (active = false) usando PUT.
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
     * Solo ADMIN.
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
            return ResponseEntity.ok(ApiResponse.of("Skin creada exitosamente", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of("Error al crear la skin: " + e.getMessage()));
        }
    }

    /**
     * Edita una skin con imagen obligatoria.
     * PUT /skins/admin/edit/{id}/with-image
     * Solo ADMIN.
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
            if (updated != null) {
                return ResponseEntity.ok(ApiResponse.of("Skin actualizada exitosamente", updated));
            }
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Skin no encontrada con id: " + id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of("Error al editar la skin: " + e.getMessage()));
        }
    }

    /**
     * Inactiva una skin (baja lógica: active = false).
     * PUT /skins/admin/inactivar/{id}
     */
    @PutMapping("/admin/inactivar/{id}")
    public ResponseEntity<?> inactivarSkin(@PathVariable Long id) {
        boolean deleted = skinService.deleteSkin(id);
        if (!deleted) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Skin no encontrada con id: " + id));
        }
        return ResponseEntity.ok(ApiResponse.of("Skin inactivada exitosamente"));
    }

    /**
     * Lista todas las skins para el panel admin.
     * GET /skins/admin/all?includeInactive=false
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllSkins(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        List<Skin> skins = skinService.getAllSkins(includeInactive);
        return ResponseEntity.ok(
                ApiResponse.of("Listado de skins (" + skins.size() + ")", skins));
    }

    // =========================================================================
    // ENDPOINTS PÚBLICOS
    // =========================================================================

    /** GET /skins/get/{id} — público */
    @GetMapping("/get/{id}")
    public ResponseEntity<?> getSkinById(@PathVariable Long id) {
        Skin skin = skinService.getSkinById(id);
        if (skin != null) {
            return ResponseEntity.ok(ApiResponse.of("Skin encontrada", skin));
        }
        return ResponseEntity.status(404)
                .body(ApiResponse.of("Skin no encontrada con id: " + id));
    }

    /** GET /skins/get/all — catálogo público (activas con stock) */
    @GetMapping("/get/all")
    public ResponseEntity<?> getAllAvailableSkins() {
        List<Skin> skins = skinService.getAllAvailableSkins();
        return ResponseEntity.ok(
                ApiResponse.of("Catálogo de skins disponibles (" + skins.size() + ")", skins));
    }

    /** GET /skins/get/category?id=1  o  ?name=Rifle */
    @GetMapping("/get/category")
    public ResponseEntity<?> getSkinsByCategory(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name) {
        if (id != null) {
            List<Skin> skins = skinService.getSkinsByCategoryId(id);
            return ResponseEntity.ok(ApiResponse.of("Skins de la categoría id " + id, skins));
        }
        if (name != null && !name.isBlank()) {
            List<Skin> skins = skinService.getSkinsByCategory(name);
            return ResponseEntity.ok(ApiResponse.of("Skins de la categoría " + name, skins));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.of("Debés especificar 'id' o 'name' como query param"));
    }

    /** GET /skins/get/price?min=10&max=100 */
    @GetMapping("/get/price")
    public ResponseEntity<?> getSkinsByPrice(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {
        if (min != null && max != null) {
            List<Skin> skins = skinService.findByRangePrice(min, max);
            return ResponseEntity.ok(ApiResponse.of("Skins entre " + min + " y " + max, skins));
        }
        if (max != null) {
            List<Skin> skins = skinService.findByPriceMax(max);
            return ResponseEntity.ok(ApiResponse.of("Skins con precio menor o igual a " + max, skins));
        }
        if (min != null) {
            List<Skin> skins = skinService.findByPriceMin(min);
            return ResponseEntity.ok(ApiResponse.of("Skins con precio mayor o igual a " + min, skins));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.of("Debés especificar 'min' y/o 'max' como query param"));
    }

    /** GET /skins/get/search?name=AK */
    @GetMapping("/get/search")
    public ResponseEntity<?> getSkinsByName(@RequestParam String name) {
        List<Skin> skins = skinService.findByName(name);
        return ResponseEntity.ok(
                ApiResponse.of("Resultados de búsqueda para: " + name, skins));
    }

    // =========================================================================
    // ENDPOINTS DE VENDEDOR (USER)
    // =========================================================================

    /**
     * GET /skins/mis-skins — lista las publicaciones del usuario autenticado.
     * Sigue funcionando para que el USER vea qué tiene publicado.
     */
    @GetMapping("/mis-skins")
    public ResponseEntity<?> misSkins(Authentication auth) {
        List<Skin> skins = skinService.getSkinsByOwner(auth.getName());
        return ResponseEntity.ok(
                ApiResponse.of("Tus skins publicadas (" + skins.size() + ")", skins));
    }

    /**
     * Edita una skin propia.
     * PUT /skins/{id}/with-image
     *
     * Sigue habilitado para que el vendedor pueda actualizar PRECIO y stock
     * de sus publicaciones existentes (las que originalmente publicó desde
     * su inventario). NO permite cambiar el catálogo asociado.
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
            if (updated != null) {
                return ResponseEntity.ok(ApiResponse.of("Skin actualizada exitosamente", updated));
            }
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Skin no encontrada con id: " + id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of("Error al editar la skin: " + e.getMessage()));
        }
    }

    /**
     * Inactiva una skin propia (baja lógica).
     * PUT /skins/{id}/inactivar
     */
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

    /**
     * ❌ ENDPOINT BLOQUEADO INTENCIONALMENTE
     *
     * POST /skins/with-image — usado anteriormente para publicar skins libres.
     *
     * Esta operación se eliminó del flujo de USER porque permitía publicar
     * skins ficticias sin tenerlas realmente en el inventario de Steam.
     *
     * Para publicar como USER, ahora hay que pasar por el inventario real:
     *   POST /inventario/sync                    ← sincroniza tu inventario de Steam
     *   GET  /inventario                         ← ves los items reales que tenés
     *   POST /inventario/{itemId}/publicar       ← publicás uno con 1 click
     *
     * Devuelve 403 con un mensaje explicativo en lugar de 404, para guiar
     * al desarrollador del frontend hacia el flujo correcto.
     */
    @PostMapping(value = "/with-image",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> publicarSkinDeshabilitado(
            Authentication auth,
            @RequestPart(value = "skin", required = false) String skinJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(403).body(ApiResponse.of(
                "Los usuarios solo pueden publicar skins de su inventario real de Steam. " +
                "Usá POST /inventario/{itemId}/publicar después de sincronizar tu inventario."
        ));
    }
}
