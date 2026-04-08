package skinsmarket.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Service
public class SkinServiceImpl implements SkinService {

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    // =========================================================================
    // CRUD base — TODOS requieren imagen
    // =========================================================================

    @Override
    public Skin getSkinById(Long id) {
        return skinRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + id));
    }

    /**
     * Crea skin con imagen BLOB obligatoria (admin).
     * Si no hay imagen lanza RuntimeException → 400 Bad Request.
     */
    @Override
    @Transactional
    public Skin createSkinWithImage(SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return buildAndSaveSkin(skinRequest, null, imageBytes);
    }

    /**
     * Edita skin actualizando la imagen BLOB obligatoria.
     */
    @Override
    @Transactional
    public Skin editSkinWithImage(Long id, SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return updateSkin(id, skinRequest, imageBytes);
    }

    /** Baja lógica: active=false. */
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
    // Listados y filtros
    // =========================================================================

    @Override
    public List<Skin> getAllSkins(boolean includeInactive) {
        if (includeInactive) return skinRepository.findAll();
        return skinRepository.findByActiveTrue();
    }

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
    // Endpoints de vendedor
    // =========================================================================

    @Override
    @Transactional
    public Skin createSkinAsVendedor(SkinRequest skinRequest, String email, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return buildAndSaveSkin(skinRequest, vendedor, imageBytes);
    }

    @Override
    @Transactional
    public Skin editSkinAsVendedor(Long id, SkinRequest skinRequest, String email, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(skinRequest);
        validarImagen(imageBytes);
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
            skin.setImage(imageBytes);
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
     * Construye y guarda una skin. La imagen es OBLIGATORIA.
     * Lanza RuntimeException si no se provee imagen.
     */
    private Skin buildAndSaveSkin(SkinRequest req, User vendedor, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarImagen(imageBytes);
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
        skin.setImage(imageBytes);

        return skinRepository.save(skin);
    }

    private Skin updateSkin(Long id, SkinRequest req, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarImagen(imageBytes);
        validarDatos(req);
        Category category = resolverCategoria(req);
        final Category finalCategory = category;

        return skinRepository.findById(id).map(skin -> {
            aplicarCampos(skin, req, finalCategory);
            skin.setImage(imageBytes);
            return skinRepository.save(skin);
        }).orElse(null);
    }

    /** Valida que la imagen no sea null ni vacía. */
    private void validarImagen(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new RuntimeException("La imagen es obligatoria para crear o editar una skin");
        }
    }

    private void validarDatos(SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        if (!InfoValidator.isValidStock(req.getStock()))   throw new NegativeStockException();
        if (!InfoValidator.isValidPrice(req.getPrice()))   throw new NegativePriceException();
        double d = req.getDiscount() != null ? req.getDiscount() : 0.0;
        if (!InfoValidator.isValidDiscount(d))             throw new InvalidDiscountException();
    }

    private Category resolverCategoria(SkinRequest req) {
        if (req.getCategoryId() == null) return null;
        return categoryRepository.findById(req.getCategoryId().longValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoría inexistente: " + req.getCategoryId()));
    }

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
