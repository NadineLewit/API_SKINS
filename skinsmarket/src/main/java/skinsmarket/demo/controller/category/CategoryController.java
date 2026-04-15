package skinsmarket.demo.controller.category;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import skinsmarket.demo.entity.Category;
import skinsmarket.demo.exception.CategoryDuplicateException;
import skinsmarket.demo.service.CategoryService;
import skinsmarket.demo.controller.category.CategoryRequest;

/**
 * Controlador REST para la gestión de Categorías de Skins.
 *
 * Las categorías permiten organizar las skins por tipo (ej: Rifle, Pistola, Cuchillo, Guante).
 *
 * Rutas públicas:    GET  /categories          (listar todas)
 * Rutas de admin:   POST /categories/create    (crear)
 *                   PUT  /categories/{id}      (editar)
 *                   DELETE /categories/{id}    (eliminar)
 */
@RestController
@RequestMapping("categories")
public class CategoryController {

    // Inyección del servicio de categorías
    @Autowired
    private CategoryService categoryService;

    /**
     * Devuelve todas las categorías con paginación opcional.
     * GET /categories
     * GET /categories?page=0&size=10
     *
     * Si no se pasan parámetros de paginación, devuelve todas las categorías.
     * Acceso público (sin autenticación).
     */
    @GetMapping
    public ResponseEntity<Page<Category>> getCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (page == null || size == null) {
            // Sin parámetros: devuelve todas las categorías en una sola página
            return ResponseEntity.ok(
                    categoryService.getCategories(PageRequest.of(0, Integer.MAX_VALUE)));
        }
        // Con parámetros: aplica paginación
        return ResponseEntity.ok(categoryService.getCategories(PageRequest.of(page, size)));
    }

    /**
     * Crea una nueva categoría de skins.
     * POST /categories/create
     * Solo accesible por usuarios con rol ADMIN.
     *
     * @throws CategoryDuplicateException si ya existe una categoría con el mismo nombre.
     */
    @PostMapping("/create")
    public ResponseEntity<Object> createCategory(
            @RequestBody CategoryRequest categoryRequest) throws CategoryDuplicateException {

        Category result = categoryService.createCategory(categoryRequest.getName());
        // Devuelve 201 Created con la URI del recurso recién creado
        return ResponseEntity.created(URI.create("/categories/" + result.getId())).body(result);
    }

    /**
     * Obtiene una categoría específica por su ID.
     * GET /categories/{id}
     * Acceso público.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        if (category != null) {
            return ResponseEntity.ok(category);
        } else {
            // Si no existe, devuelve 404 Not Found
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Edita una categoría existente.
     * PUT /categories/{id}
     * Solo accesible por usuarios con rol ADMIN.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Category> editCategory(@PathVariable Long id,
                                                 @RequestBody Category category) {
        Category updatedCategory = categoryService.editCategory(id, category);
        if (updatedCategory != null) {
            return ResponseEntity.ok(updatedCategory);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina una categoría por su ID.
     * DELETE /categories/{id}
     * Solo accesible por usuarios con rol ADMIN.
     * Devuelve 204 No Content al eliminar correctamente.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}