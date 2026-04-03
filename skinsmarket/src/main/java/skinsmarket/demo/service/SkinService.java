package skinsmarket.demo.service;

import skinsmarket.demo.controller.skin.SkinRequest;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;

import java.util.List;

/**
 * Interfaz del servicio de Skins.
 *
 */
public interface SkinService {

    /** Obtiene una skin por su ID. */
    Skin getSkinById(Long id);

    /**
     * Crea una nueva skin (ABM de admin).
     * @throws NegativeStockException    si el stock es negativo
     * @throws NegativePriceException    si el precio es <= 0
     * @throws InvalidDiscountException  si el descuento está fuera de [0,1]
     */
    Skin createSkin(SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    /**
     * Edita una skin existente (ABM de admin).
     * @throws NegativeStockException    si el stock es negativo
     * @throws NegativePriceException    si el precio es <= 0
     * @throws InvalidDiscountException  si el descuento está fuera de [0,1]
     */
    Skin editSkin(Long id, SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    /**
     * Baja lógica de una skin (active = false).
     * @return true si la skin existía y fue desactivada, false si el ID no existe
     */
    boolean deleteSkin(Long id);

    /** Devuelve todas las skins (activas e inactivas) para el panel de admin. */
    List<Skin> getAllSkins();

    /** Devuelve solo las skins con stock > 0 y active = true para el catálogo público. */
    List<Skin> getAllAvailableSkins();

    /** Filtra skins por nombre de categoría. */
    List<Skin> getSkinsByCategory(String categoryName);

    /** Filtra skins por rango de precio [min, max]. */
    List<Skin> findByRangePrice(Double min, Double max);

    /** Filtra skins con precio <= max. */
    List<Skin> findByPriceMax(Double max);

    /** Filtra skins con precio >= min. */
    List<Skin> findByPriceMin(Double min);

    /** Busca skins cuyo nombre contenga el texto dado (case-insensitive). */
    List<Skin> findByName(String name);

    /** Devuelve las skins publicadas por un usuario (vendedor). */
    List<Skin> getSkinsByOwner(String email);

    /**
     * Crea una skin asignando al usuario autenticado como vendedor.
     * Equivalente a createSkin() pero con vendedor = usuario autenticado.
     */
    Skin createSkinAsVendedor(SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    /**
     * Edita una skin solo si le pertenece al usuario autenticado.
     * Lanza RuntimeException (403) si el usuario no es el vendedor de la skin.
     */
    Skin editSkinAsVendedor(Long id, SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    /**
     * Baja lógica de una skin solo si le pertenece al usuario autenticado.
     * @return true si se desactivó, false si el ID no existe
     */
    boolean deleteSkinAsVendedor(Long id, String email);
}