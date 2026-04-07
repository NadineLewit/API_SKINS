package skinsmarket.demo.service;

import skinsmarket.demo.controller.skin.SkinRequest;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;

import java.util.List;

public interface SkinService {

    Skin getSkinById(Long id);

    // Sin imagen
    Skin createSkin(SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    // Con imagen BLOB (nuevo — pedido por la profe)
    Skin createSkinWithImage(SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    Skin editSkin(Long id, SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    // Con imagen BLOB (nuevo — pedido por la profe)
    Skin editSkinWithImage(Long id, SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    boolean deleteSkin(Long id);

    List<Skin> getAllSkins(boolean includeInactive);
    List<Skin> getAllAvailableSkins();
    List<Skin> getSkinsByCategory(String categoryName);
    List<Skin> getSkinsByCategoryId(Integer categoryId);
    List<Skin> findByRangePrice(Double min, Double max);
    List<Skin> findByPriceMax(Double max);
    List<Skin> findByPriceMin(Double min);
    List<Skin> findByName(String name);
    List<Skin> getSkinsByOwner(String email);

    Skin createSkinAsVendedor(SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    Skin editSkinAsVendedor(Long id, SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException;

    boolean deleteSkinAsVendedor(Long id, String email);
}
