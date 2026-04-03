package skinsmarket.demo.controller.skin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.service.SkinService;

/**
 * Controlador REST para la gestión de Skins (artículos cosméticos de videojuegos).
 *
 * Sigue la misma estructura que GameController del TPO aprobado.
 * Rutas públicas: GET /skins/get/**
 * Rutas de admin: POST/PUT/DELETE /skins/admin/**
 * Rutas de usuario autenticado: POST /skins (publicar skin propia)
 */
@RestController
@RequestMapping("skins")
public class SkinController {

    // Inyección del servicio de skins mediante @Autowired (consistente con el TPO aprobado)
    @Autowired
    private SkinService skinService;

    // -------------------------------------------------------------------------
    // ENDPOINTS DE ADMINISTRADOR
    // -------------------------------------------------------------------------

    /**
     * Crea una skin desde el panel de administración (sin imagen).
     * POST /skins/admin/create
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PostMapping("/admin/create")
    public ResponseEntity<Skin> createSkin(@RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        Skin result = skinService.createSkin(skinRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * Crea una skin con imagen desde el panel de administración.
     * POST /skins/admin/create-with-image
     * Recibe multipart/form-data con los campos de la skin y el archivo de imagen.
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PostMapping("/admin/create-with-image")
    public ResponseEntity<?> createSkinWithImage(
            @RequestParam("name") String name,
            @RequestParam("price") Double price,
            @RequestParam("stock") Integer stock,
            @RequestParam("game") String game,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("imagen") MultipartFile imagen
    ) {
        try {
            // 1. Guardar la imagen en la carpeta local de uploads
            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            String originalName = imagen.getOriginalFilename();
            // Sanitizar el nombre del archivo para evitar caracteres peligrosos
            String safeOriginal = (originalName == null) ? "file"
                    : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + safeOriginal;
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, imagen.getBytes());

            // 2. Construir la URL pública de la imagen (URL-encoded para evitar problemas con espacios)
            String encoded = java.net.URLEncoder
                    .encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            String imagenUrl = "http://localhost:4002/uploads/" + encoded;

            // 3. Armar el SkinRequest con todos los datos recibidos
            SkinRequest skinRequest = new SkinRequest();
            skinRequest.setName(name);
            skinRequest.setPrice(price);
            skinRequest.setStock(stock);
            skinRequest.setGame(game);
            skinRequest.setCategoryId(categoryId);
            skinRequest.setImageUrl(imagenUrl);

            // 4. Delegar la creación al servicio
            Skin result = skinService.createSkin(skinRequest);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al crear la skin: " + e.getMessage());
        }
    }

    /**
     * Edita una skin existente (sin imagen) desde el panel de administración.
     * PUT /skins/admin/{id}
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PutMapping("/admin/{id}")
    public ResponseEntity<Skin> editSkin(@PathVariable Long id,
                                         @RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        Skin updatedSkin = skinService.editSkin(id, skinRequest);
        if (updatedSkin != null) {
            return ResponseEntity.ok(updatedSkin);
        } else {
            // Si no se encontró la skin, devolvemos 404
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Edita una skin con imagen desde el panel de administración.
     * PUT /skins/admin/{id}/edit-with-image
     * Recibe multipart/form-data con JSON de la skin y el nuevo archivo de imagen.
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PutMapping(value = "/admin/{id}/edit-with-image",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> editSkinWithImage(
            @PathVariable Long id,
            @RequestPart("skin") String skinJson,
            @RequestPart("image") MultipartFile image
    ) {
        try {
            // Guardar la nueva imagen en la carpeta de uploads
            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            String originalName = image.getOriginalFilename();
            String safeOriginal = (originalName == null) ? "file"
                    : originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String fileName = System.currentTimeMillis() + "_" + safeOriginal;
            Path filePath = Paths.get(uploadDir + fileName);
            Files.write(filePath, image.getBytes());

            String encoded = java.net.URLEncoder
                    .encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            String imageUrl = "http://localhost:4002/uploads/" + encoded;

            // Parsear el JSON de la skin y setear la nueva URL de imagen
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            SkinRequest skinRequest = mapper.readValue(skinJson, SkinRequest.class);
            skinRequest.setImageUrl(imageUrl);

            Skin updated = skinService.editSkin(id, skinRequest);
            if (updated != null) return ResponseEntity.ok(updated);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al editar la skin: " + e.getMessage());
        }
    }

    /**
     * Elimina (baja lógica) una skin desde el panel de administración.
     * DELETE /skins/admin/{id}
     * Devuelve 204 si se desactivó correctamente, 404 si el ID no existe.
     * Solo accesible por usuarios con rol ADMIN.
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteSkin(@PathVariable Long id) {
        boolean deleted = skinService.deleteSkin(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve todas las skins (incluyendo inactivas), para el panel de administración.
     * GET /skins/admin
     * Solo accesible por usuarios con rol ADMIN.
     */
    @GetMapping("/admin")
    public ResponseEntity<List<Skin>> getAllSkins() {
        List<Skin> skins = skinService.getAllSkins();
        return ResponseEntity.ok(skins);
    }

    // -------------------------------------------------------------------------
    // ENDPOINTS PÚBLICOS (accesibles sin autenticación)
    // -------------------------------------------------------------------------

    /**
     * Devuelve una skin por su ID.
     * GET /skins/get/{id}
     * Acceso público.
     */
    @GetMapping("/get/{id}")
    public ResponseEntity<Skin> getSkinById(@PathVariable Long id) {
        Skin skin = skinService.getSkinById(id);
        if (skin != null) {
            return ResponseEntity.ok(skin);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Devuelve todas las skins activas (disponibles para compra).
     * GET /skins/get/available
     * Acceso público.
     */
    @GetMapping("/get/available")
    public ResponseEntity<List<Skin>> getAllAvailableSkins() {
        List<Skin> skins = skinService.getAllAvailableSkins();
        return ResponseEntity.ok(skins);
    }

    /**
     * Devuelve skins filtradas por categoría.
     * GET /skins/get/category?name=Cuchillos
     * Acceso público.
     */
    @GetMapping("/get/category")
    public ResponseEntity<List<Skin>> getSkinsByCategory(@RequestParam String name) {
        List<Skin> skins = skinService.getSkinsByCategory(name);
        return ResponseEntity.ok(skins);
    }

    /**
     * Devuelve skins filtradas por rango de precio.
     * GET /skins/get/price?min=5&max=100
     * Parámetros opcionales: si solo se pasa min o max, filtra en una sola dirección.
     * Acceso público.
     */
    @GetMapping("/get/price")
    public ResponseEntity<List<Skin>> getSkinsByPrice(
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max) {

        if (min != null && max != null) {
            return ResponseEntity.ok(skinService.findByRangePrice(min, max));
        } else if (max != null) {
            return ResponseEntity.ok(skinService.findByPriceMax(max));
        } else if (min != null) {
            return ResponseEntity.ok(skinService.findByPriceMin(min));
        } else {
            // Si no se pasa ningún parámetro, la request es inválida
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Devuelve skins filtradas por nombre (búsqueda parcial).
     * GET /skins/get/name?name=Dragon
     * Acceso público.
     */
    @GetMapping("/get/name")
    public ResponseEntity<List<Skin>> getSkinsByName(@RequestParam String name) {
        return ResponseEntity.ok(skinService.findByName(name));
    }

    // -------------------------------------------------------------------------
    // ENDPOINTS DE VENDEDOR (accesibles por cualquier USER autenticado)
    // El TPO dice: "Los usuarios registrados como vendedores podrán realizar
    // el alta de una publicación de su producto y gestionarla".
    // -------------------------------------------------------------------------

    /**
     * Publica una nueva skin (cualquier usuario autenticado puede vender).
     * POST /skins
     *
     * El sistema registra automáticamente al usuario autenticado como vendedor.
     * Valida precio, stock y descuento igual que el endpoint de admin.
     */
    @PostMapping
    public ResponseEntity<Skin> publicarSkin(
            Authentication auth,
            @RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        // Setear el email del vendedor en el request para que el service lo asigne
        Skin result = skinService.createSkinAsVendedor(skinRequest, auth.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * Edita una skin propia del usuario autenticado (solo el vendedor puede editarla).
     * PUT /skins/{id}
     *
     * Lanza 403 si el usuario intenta editar una skin que no le pertenece.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Skin> editarMiSkin(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        Skin updated = skinService.editSkinAsVendedor(id, skinRequest, auth.getName());
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Elimina (baja lógica) una skin propia del usuario autenticado.
     * DELETE /skins/{id}
     *
     * Solo el vendedor que publicó la skin puede eliminarla.
     * Lanza 403 si intenta eliminar una skin ajena.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMiSkin(
            Authentication auth,
            @PathVariable Long id) {
        boolean deleted = skinService.deleteSkinAsVendedor(id, auth.getName());
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Devuelve todas las skins publicadas por el usuario autenticado.
     * GET /skins/mis-skins
     * Requiere autenticación (rol USER o ADMIN).
     */
    @GetMapping("/mis-skins")
    public ResponseEntity<List<Skin>> misSkins(Authentication auth) {
        // Obtenemos el email del usuario autenticado desde el contexto de seguridad
        String email = auth.getName();
        List<Skin> skins = skinService.getSkinsByOwner(email);
        return ResponseEntity.ok(skins);
    }
}