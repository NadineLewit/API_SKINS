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
 * CAMBIOS (pedido por la profe):
 *   - @Transactional agregado en createCategory y deleteCategory (atomicidad ACID).
 *     editCategory ya lo tenía. Ahora todos los métodos de escritura son transaccionales.
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Devuelve todas las categorías paginadas.
     * GET /categories?page=0&size=10
     */
    @Override
    public Page<Category> getCategories(PageRequest pageable) {
        return categoryRepository.findAll(pageable);
    }

    /**
     * Obtiene una categoría por ID.
     * Usa Optional con orElseThrow (pedido por la profe).
     */
    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
    }

    /**
     * Crea una nueva categoría verificando que no haya duplicados.
     *
     * @Transactional: si el save() falla, no queda ningún estado inconsistente (ACID).
     * @throws CategoryDuplicateException si ya existe una categoría con el mismo nombre.
     */
    @Override
    @Transactional
    public Category createCategory(String name) throws CategoryDuplicateException {
        // Validar duplicado case-insensitive
        List<Category> existing = categoryRepository.findByNameIgnoreCase(name);
        if (!existing.isEmpty()) {
            throw new CategoryDuplicateException();
        }
        return categoryRepository.save(new Category(name));
    }

    /**
     * Elimina una categoría por ID.
     *
     * @Transactional: garantiza que la eliminación sea atómica.
     */
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    /**
     * Edita el nombre de una categoría existente.
     *
     * Usa Optional con map().orElse(null) — si no existe devuelve null
     * y el controller responde 404.
     *
     * @Transactional: garantiza que el update sea atómico.
     */
    @Override
    @Transactional
    public Category editCategory(Long id, Category categoryDetails) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(categoryDetails.getName());
            return categoryRepository.save(category);
        }).orElse(null);
    }
}
