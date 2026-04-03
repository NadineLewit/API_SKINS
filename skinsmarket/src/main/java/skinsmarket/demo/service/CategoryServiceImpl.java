package skinsmarket.demo.service;

import skinsmarket.demo.exception.CategoryDuplicateException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import skinsmarket.demo.entity.Category;
import skinsmarket.demo.repository.CategoryRepository;

import java.util.List;

/**
 * Implementación del servicio de Categorías de skins.
 *
 * Estructura idéntica al CategoryServiceImpl del TPO aprobado.
 * Se restaura la validación de duplicados (estaba comentada en el TPO)
 * para usar la excepción CategoryDuplicateException correctamente.
 *
 * Usa @Autowired en atributos (estilo del TPO aprobado).
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    // Repositorio inyectado por @Autowired (consistente con el TPO aprobado)
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Devuelve todas las categorías paginadas.
     * Delega directamente al repositorio JPA (findAll con Pageable).
     */
    @Override
    public Page<Category> getCategories(PageRequest pageable) {
        return categoryRepository.findAll(pageable);
    }

    /**
     * Obtiene una categoría por su ID.
     * Lanza IllegalArgumentException si no existe (Spring convierte a 500,
     * idealmente se manejaría con una excepción propia en una versión futura).
     */
    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
    }

    /**
     * Crea una nueva categoría verificando que el nombre no esté duplicado.
     *
     * Se restaura la validación de duplicados que estaba comentada en el TPO aprobado.
     * Lanza CategoryDuplicateException si ya existe una categoría con el mismo nombre.
     *
     * @throws CategoryDuplicateException si el nombre ya existe en la base de datos
     */
    @Override
    public Category createCategory(String name) throws CategoryDuplicateException {
        // Verificar duplicado ignorando mayúsculas/minúsculas.
        // Sin esto, "Rifle" y "rifle" se crearían como dos categorías distintas,
        // lo que confundiría los filtros de búsqueda del catálogo.
        List<Category> existing = categoryRepository.findByNameIgnoreCase(name);
        if (!existing.isEmpty()) {
            throw new CategoryDuplicateException();
        }
        return categoryRepository.save(new Category(name));
    }

    /**
     * Elimina una categoría por su ID.
     * Delega directamente al repositorio.
     */
    @Override
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    /**
     * Edita el nombre de una categoría existente.
     *
     * Usa el patrón map().orElse(null) del TPO aprobado:
     * si la categoría existe la actualiza, si no devuelve null
     * (el controller devuelve 404 en ese caso).
     *
     * @Transactional garantiza que el save() ocurra dentro de la misma transacción.
     */
    @Override
    @Transactional
    public Category editCategory(Long id, Category categoryDetails) {
        return categoryRepository.findById(id).map(category -> {
            // Solo actualizamos el nombre (único campo editable)
            category.setName(categoryDetails.getName());
            return categoryRepository.save(category);
        }).orElse(null);
    }
}