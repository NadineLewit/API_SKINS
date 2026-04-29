package skinsmarket.demo.controller.category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.Category;
import skinsmarket.demo.exception.CategoryDuplicateException;
import skinsmarket.demo.service.CategoryService;

/**
 * Controlador REST para la gestión de Categorías de Skins.
 *
 * Las categorías permiten organizar las skins por tipo (ej: Rifle, Pistola, Cuchillo, Guante).
 *
 * Rutas públicas:    GET  /categories          (listar todas)
 *                    GET  /categories/{id}     (obtener una)
 * Rutas de admin:    POST /categories/create   (crear)
 *                    PUT  /categories/{id}     (editar)
 *                    DELETE /categories/{id}   (eliminar — acá SÍ es delete físico)
 *
 * Todas las respuestas siguen el formato uniforme ApiResponse:
 *   { "message": "...", "data": { ... opcional ... } }
 */
@RestController
@RequestMapping("categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Devuelve todas las categorías con paginación opcional.
     * GET /categories
     * GET /categories?page=0&size=10
     *
     * Acceso público (sin autenticación).
     */
    @GetMapping
    public ResponseEntity<?> getCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Page<Category> result;
        if (page == null || size == null) {
            result = categoryService.getCategories(PageRequest.of(0, Integer.MAX_VALUE));
        } else {
            result = categoryService.getCategories(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(
                ApiResponse.of("Listado de categorías (" + result.getTotalElements() + ")", result));
    }

    /**
     * Crea una nueva categoría de skins.
     * POST /categories/create
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCategory(
            @RequestBody CategoryRequest categoryRequest) throws CategoryDuplicateException {
        Category result = categoryService.createCategory(categoryRequest.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.of("Categoría creada exitosamente", result));
    }

    /**
     * Obtiene una categoría específica por su ID.
     * GET /categories/{id}
     * Acceso público.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        if (category != null) {
            return ResponseEntity.ok(ApiResponse.of("Categoría encontrada", category));
        }
        return ResponseEntity.status(404)
                .body(ApiResponse.of("Categoría no encontrada con id: " + id));
    }

    /**
     * Edita una categoría existente.
     * PUT /categories/{id}
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> editCategory(@PathVariable Long id,
                                          @RequestBody Category category) {
        Category updatedCategory = categoryService.editCategory(id, category);
        if (updatedCategory != null) {
            return ResponseEntity.ok(
                    ApiResponse.of("Categoría actualizada exitosamente", updatedCategory));
        }
        return ResponseEntity.status(404)
                .body(ApiResponse.of("Categoría no encontrada con id: " + id));
    }

    /**
     * Elimina una categoría por su ID.
     * DELETE /categories/{id}
     * Solo accesible por usuarios con rol ADMIN.
     *
     * Acá SÍ es delete físico, no aplica baja lógica (las categorías no tienen
     * FK que rompa al eliminarlas, ya que las skins permiten category_id null).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.of("Categoría eliminada exitosamente"));
    }
}
