package skinsmarket.demo.service;

import skinsmarket.demo.controller.skincatalogo.SkinCatalogoRequest;
import skinsmarket.demo.entity.SkinCatalogo;

import java.util.List;

public interface SkinCatalogoService {

    /**
     * Importa skins desde la API pública de ByMykel/CSGO-API y las persiste en la BD.
     * Si una skin ya existe (matcheada por externalId) se omite, no se duplica.
     *
     * @param limit cantidad máxima de skins a importar en esta corrida (0 o null = todas).
     *              Recomendado limitarlo para no traer 30K registros la primera vez.
     * @return cantidad de skins efectivamente insertadas en la BD.
     */
    int sincronizarDesdeApi(Integer limit);

    /** Crea manualmente un item del catálogo (para casos especiales del ADMIN). */
    SkinCatalogo crear(SkinCatalogoRequest request);

    /** Lista todo el catálogo. */
    List<SkinCatalogo> listar();

    /** Obtiene un item del catálogo por su ID interno. */
    SkinCatalogo obtenerPorId(Long id);

    /** Búsqueda por nombre parcial. */
    List<SkinCatalogo> buscarPorNombre(String nombre);

    /** Filtro por arma (ej: "AK-47"). */
    List<SkinCatalogo> filtrarPorArma(String weapon);

    /** Filtro por categoría (ej: "Rifles"). */
    List<SkinCatalogo> filtrarPorCategoria(String categoria);

    /** Elimina un item del catálogo por su ID. */
    void eliminar(Long id);
}
