package skinsmarket.demo.service;

import skinsmarket.demo.controller.skin.SkinRequest;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;

import java.util.List;

/**
 * SkinService — sin métodos de manejo de imágenes (BLOB removido) y sin
 * relación con la antigua entidad Category. Las "categorías" se derivan
 * ahora del catálogo (catalogo.categoryName).
 */
public interface SkinService {

    Skin getSkinById(Long id);

    Skin createSkin(SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    Skin editSkin(Long id, SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    boolean deleteSkin(Long id);

    Skin editSkinAsVendedor(Long id, SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    boolean deleteSkinAsVendedor(Long id, String email);

    // ─── Listados y filtros ─────────────────────────────────────────────────

    List<Skin> getAllSkins(boolean includeInactive);

    List<Skin> getAllAvailableSkins();

    /**
     * Filtra skins por nombre de categoría (rifle, pistol, knife, smg, etc).
     * La categoría sale del catálogo asociado a la skin.
     */
    List<Skin> getSkinsByCategoryName(String categoryName);

    List<Skin> findByRangePrice(Double min, Double max);

    List<Skin> findByPriceMax(Double max);

    List<Skin> findByPriceMin(Double min);

    List<Skin> findByName(String name);

    List<Skin> getSkinsByOwner(String email);
}
