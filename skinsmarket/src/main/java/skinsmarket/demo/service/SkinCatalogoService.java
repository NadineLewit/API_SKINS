package skinsmarket.demo.service;

import skinsmarket.demo.controller.skincatalogo.SkinCatalogoRequest;
import skinsmarket.demo.entity.SkinCatalogo;

import java.util.List;

/**
 * Servicio del catálogo maestro de skins reales de CS2.
 *
 * Las skins del catálogo se importan desde la API pública de ByMykel/CSGO-API
 * (~22.000 skins). El catálogo se usa para:
 *   - Que un USER al publicar una skin desde su inventario herede los datos
 *     reales (nombre, descripción, imagen).
 *   - Que el catálogo público (/catalogo) sea explorable por cualquier visitante.
 */
public interface SkinCatalogoService {

    /**
     * Sincroniza el catálogo desde la API de ByMykel.
     *
     * @param limit cantidad máxima de skins a importar.
     *              Si limit ≤ 0, importa TODAS las skins disponibles (~22.000).
     * @return cantidad de skins efectivamente insertadas (no actualizaciones).
     */
    int sincronizarDesdeApi(int limit);

    List<SkinCatalogo> listarTodos();

    SkinCatalogo obtenerPorId(Long id);

    List<SkinCatalogo> buscarPorNombre(String nombre);

    /**
     * Filtra el catálogo por arma y/o categoría.
     * Si ambos son null/blank → devuelve lista vacía.
     */
    List<SkinCatalogo> filtrar(String arma, String categoria);

    SkinCatalogo crear(SkinCatalogoRequest request);

    boolean eliminar(Long id);
}
