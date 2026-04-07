package skinsmarket.demo.service;

import skinsmarket.demo.controller.skin.SkinRequest;
import skinsmarket.demo.entity.Category;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;
import skinsmarket.demo.repository.CategoryRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.utils.InfoValidator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del servicio de Skins.
 *
 * CAMBIOS (pedido por la profe):
 *   - createSkinWithImage / editSkinWithImage: nuevos métodos que reciben byte[]
 *     y los almacenan como BLOB en la BD (antes se guardaba en disco con imageUrl).
 *   - Se refuerza el uso de Optional y Streams según la profe.
 */
@Service
public class SkinServiceImpl implements SkinService {

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    // =========================================================================
    // CRUD base
    // =========================================================================

    /** Busca por ID usando Optional — lanza excepción si no existe. */
    @Override
    public Skin getSkinById(Long id) {
        return skinRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + id));
    }

    /**
     * Crea skin sin imagen (admin).
     * @Transactional: si algo falla, no queda stock descontado ni categoría asignada a medias.
     */
    @Override
    @Transactional
    public Skin createSkin(SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return buildAndSaveSkin(skinRequest, null, null);
    }

    /**
     * Crea skin con imagen almacenada como BLOB en la BD (pedido por la profe).
     *
     * @param imageBytes bytes del archivo recibido como multipart/form-data
     */
    @Override
    @Transactional
    public Skin createSkinWithImage(SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return buildAndSaveSkin(skinRequest, null, imageBytes);
    }

    /** Edita skin sin imagen. */
    @Override
    @Transactional
    public Skin editSkin(Long id, SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return updateSkin(id, skinRequest, null, false);
    }

    /**
     * Edita skin actualizando también la imagen BLOB.
     *
     * @param imageBytes nuevos bytes de imagen; si es null, la imagen no se modifica
     */
    @Override
    @Transactional
    public Skin editSkinWithImage(Long id, SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return updateSkin(id, skinRequest, imageBytes, true);
    }

    /** Baja lógica: active=false. Devuelve false si el ID no existe. */
    @Override
    @Transactional
    public boolean deleteSkin(Long id) {
        return skinRepository.findById(id).map(skin -> {
            skin.setActive(false);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    // =========================================================================
    // Listados y filtros (usan Streams — pedido por la profe)
    // =========================================================================

    @Override
    public List<Skin> getAllSkins(boolean includeInactive) {
        if (includeInactive) return skinRepository.findAll();
        return skinRepository.findByActiveTrue();
    }

    /**
     * Catálogo público: solo activas con stock > 0.
     * Usa Stream + filter (pedido por la profe).
     */
    @Override
    public List<Skin> getAllAvailableSkins() {
        return skinRepository.findByActiveTrue()
                .stream()
                .filter(s -> s.getStock() > 0)
                .toList();
    }

    @Override
    public List<Skin> getSkinsByCategory(String categoryName) {
        return skinRepository.findByCategory_Name(categoryName);
    }

    @Override
    public List<Skin> getSkinsByCategoryId(Integer categoryId) {
        return skinRepository.findByCategory_Id(categoryId);
    }

    @Override
    public List<Skin> findByRangePrice(Double min, Double max) {
        return skinRepository.findByPriceBetween(min, max);
    }

    @Override
    public List<Skin> findByPriceMax(Double max) {
        return skinRepository.findByPriceLessThanEqual(max);
    }

    @Override
    public List<Skin> findByPriceMin(Double min) {
        return skinRepository.findByPriceGreaterThanEqual(min);
    }

    @Override
    public List<Skin> findByName(String name) {
        return skinRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public List<Skin> getSkinsByOwner(String email) {
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return skinRepository.findByVendedor(vendedor);
    }

    // =========================================================================
    // Endpoints de vendedor (USER autenticado)
    // =========================================================================

    @Override
    @Transactional
    public Skin createSkinAsVendedor(SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return buildAndSaveSkin(skinRequest, vendedor, null);
    }

    @Override
    @Transactional
    public Skin editSkinAsVendedor(Long id, SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {

        validarDatos(skinRequest);
        Category category = resolverCategoria(skinRequest);
        final Category finalCategory = category;

        return skinRepository.findById(id).map(skin -> {
            User usuario = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            boolean esAdmin = usuario.getRole().name().equals("ADMIN");
            boolean esVendedor = skin.getVendedor() != null
                    && skin.getVendedor().getEmail().equals(email);
            if (!esAdmin && !esVendedor) {
                throw new RuntimeException("No tenés permiso para editar esta skin");
            }
            aplicarCampos(skin, skinRequest, finalCategory);
            return skinRepository.save(skin);
        }).orElse(null);
    }

    @Override
    @Transactional
    public boolean deleteSkinAsVendedor(Long id, String email) {
        return skinRepository.findById(id).map(skin -> {
            User usuario = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            boolean esAdmin = usuario.getRole().name().equals("ADMIN");
            boolean esVendedor = skin.getVendedor() != null
                    && skin.getVendedor().getEmail().equals(email);
            if (!esAdmin && !esVendedor) {
                throw new RuntimeException("No tenés permiso para eliminar esta skin");
            }
            skin.setActive(false);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    /**
     * Construye y guarda una skin nueva con sus datos, vendedor e imagen.
     * Centraliza la lógica para no repetir código en create/createWithImage/createAsVendedor.
     */
    private Skin buildAndSaveSkin(SkinRequest req, User vendedor, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(req);
        Category category = resolverCategoria(req);
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;

        Skin skin = new Skin();
        skin.setName(req.getName());
        skin.setDescription(req.getDescription());
        skin.setPrice(req.getPrice());
        skin.setDiscount(discount);
        skin.setStock(req.getStock());
        skin.setGame(req.getGame());
        skin.setCategory(category);
        skin.setActive(true);
        skin.setFechaAlta(LocalDateTime.now());
        skin.setVendedor(vendedor);
        skin.setImage(imageBytes);        // BLOB — null si no se sube imagen

        return skinRepository.save(skin);
    }

    /**
     * Actualiza campos de una skin existente.
     *
     * @param updateImage si true, actualiza el campo image con imageBytes;
     *                    si false, deja la imagen existente sin tocar.
     */
    private Skin updateSkin(Long id, SkinRequest req, byte[] imageBytes, boolean updateImage)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(req);
        Category category = resolverCategoria(req);
        final Category finalCategory = category;

        return skinRepository.findById(id).map(skin -> {
            aplicarCampos(skin, req, finalCategory);
            if (updateImage) {
                skin.setImage(imageBytes);
            }
            return skinRepository.save(skin);
        }).orElse(null);
    }

    /** Valida stock, precio y descuento. Lanza excepción si alguno es inválido. */
    private void validarDatos(SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        if (!InfoValidator.isValidStock(req.getStock()))   throw new NegativeStockException();
        if (!InfoValidator.isValidPrice(req.getPrice()))   throw new NegativePriceException();
        double d = req.getDiscount() != null ? req.getDiscount() : 0.0;
        if (!InfoValidator.isValidDiscount(d))             throw new InvalidDiscountException();
    }

    /** Busca la categoría por ID usando Optional. */
    private Category resolverCategoria(SkinRequest req) {
        if (req.getCategoryId() == null) return null;
        return categoryRepository.findById(req.getCategoryId().longValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoría inexistente: " + req.getCategoryId()));
    }

    /** Aplica los campos del request a la entidad skin existente. */
    private void aplicarCampos(Skin skin, SkinRequest req, Category category) {
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;
        skin.setName(req.getName());
        skin.setDescription(req.getDescription());
        skin.setPrice(req.getPrice());
        skin.setDiscount(discount);
        skin.setStock(req.getStock());
        skin.setGame(req.getGame());
        skin.setCategory(category);
    }
}
