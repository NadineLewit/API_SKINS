package skinsmarket.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import skinsmarket.demo.entity.Category;
import skinsmarket.demo.exception.CategoryDuplicateException;

/**
 * Interfaz del servicio de Categorías.
 *
 * Define el contrato que implementa CategoryServiceImpl.
 * para la capa de servicios.
 */
public interface CategoryService {

    /** Devuelve las categorías paginadas. */
    Page<Category> getCategories(PageRequest pageRequest);

    /** Obtiene una categoría por su ID. */
    Category getCategoryById(Long id);

    /**
     * Crea una nueva categoría.
     * @throws CategoryDuplicateException si ya existe una con ese nombre
     */
    Category createCategory(String name) throws CategoryDuplicateException;

    /** Edita el nombre de una categoría existente. */
    Category editCategory(Long id, Category category);

    /** Elimina una categoría por su ID. */
    void deleteCategory(Long id);
}
