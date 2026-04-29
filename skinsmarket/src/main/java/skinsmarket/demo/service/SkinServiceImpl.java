package skinsmarket.demo.service;

import skinsmarket.demo.controller.skin.SkinRequest;
import skinsmarket.demo.entity.Category;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;
import skinsmarket.demo.repository.CategoryRepository;
import skinsmarket.demo.repository.SkinCatalogoRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.utils.InfoValidator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación del SkinService.
 *
 * REGLA DE NEGOCIO (cambio reciente):
 *   - USER vendedor: para publicar una skin DEBE referenciar un SkinCatalogo
 *     existente (catalogoId obligatorio). Los atributos visuales (name,
 *     description, game) se copian del catálogo, ignorando lo que el usuario
 *     haya enviado en esos campos.
 *   - ADMIN: catalogoId opcional. Si lo provee, mismo comportamiento que USER.
 *     Si no lo provee, crea/edita libremente con los datos enviados.
 */
@Service
public class SkinServiceImpl implements SkinService {

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkinCatalogoRepository skinCatalogoRepository;

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
     * El catalogoId es OPCIONAL para admin — puede crear libremente o sobre el catálogo.
     */
    @Override
    @Transactional
    public Skin createSkinWithImage(SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        // ADMIN: catalogoId es opcional → false en el segundo argumento
        return buildAndSaveSkin(skinRequest, null, imageBytes, false);
    }

    /** Edita skin (admin). */
    @Override
    @Transactional
    public Skin editSkinWithImage(Long id, SkinRequest skinRequest, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return updateSkin(id, skinRequest, imageBytes, false);
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
    // Endpoints de vendedor (USER)
    // =========================================================================

    @Override
    @Transactional
    public Skin createSkinAsVendedor(SkinRequest skinRequest, String email, byte[] imageBytes)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        // USER: catalogoId obligatorio → true en el segundo argumento (requiereCatalogo)
        return buildAndSaveSkin(skinRequest, vendedor, imageBytes, true);
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
            // En la edición NO se permite cambiar la skin del catálogo (no tiene sentido
            // que una publicación de "AK-47 Redline" pase a ser "AWP Dragon Lore"). Se
            // ignora el catalogoId del request — solo se actualizan precio, stock, etc.
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
                throw new RuntimeException("No tenés permiso para inactivar esta skin");
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
     * Construye y guarda una skin (publicación).
     *
     * @param req               datos de la skin
     * @param vendedor          el usuario vendedor (null si es ADMIN sin email)
     * @param imageBytes        imagen obligatoria
     * @param requiereCatalogo  true para USER (catalogoId obligatorio),
     *                          false para ADMIN (catalogoId opcional)
     */
    private Skin buildAndSaveSkin(SkinRequest req, User vendedor, byte[] imageBytes,
                                  boolean requiereCatalogo)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarImagen(imageBytes);
        validarDatos(req);
        Category category = resolverCategoria(req);
        SkinCatalogo catalogo = resolverCatalogo(req, requiereCatalogo);

        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;

        Skin skin = new Skin();
        skin.setPrice(req.getPrice());
        skin.setDiscount(discount);
        skin.setStock(req.getStock());
        skin.setCategory(category);
        skin.setActive(true);
        skin.setFechaAlta(LocalDateTime.now());
        skin.setVendedor(vendedor);
        skin.setImage(imageBytes);
        skin.setCatalogo(catalogo);

        // Si hay catálogo, los datos visuales se copian del catálogo (ignorando lo
        // que el usuario envió). Si no hay catálogo (caso ADMIN libre), se usan los
        // valores del request.
        if (catalogo != null) {
            skin.setName(catalogo.getName());
            skin.setDescription(catalogo.getDescription());
            skin.setGame("CS2"); // el catálogo de ByMykel es de CS2
        } else {
            skin.setName(req.getName());
            skin.setDescription(req.getDescription());
            skin.setGame(req.getGame());
        }

        // Atributos del dominio que sí decide el vendedor (varían por publicación)
        if (req.getRareza() != null && !req.getRareza().isBlank()) {
            skin.setRareza(Skin.Rareza.valueOf(req.getRareza().toUpperCase()));
        }
        if (req.getExterior() != null && !req.getExterior().isBlank()) {
            skin.setExterior(Skin.Exterior.valueOf(req.getExterior().toUpperCase()));
        }
        skin.setStattrak(Boolean.TRUE.equals(req.getStattrak()));

        return skinRepository.save(skin);
    }

    private Skin updateSkin(Long id, SkinRequest req, byte[] imageBytes, boolean requiereCatalogo)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarImagen(imageBytes);
        validarDatos(req);
        Category category = resolverCategoria(req);
        final Category finalCategory = category;

        return skinRepository.findById(id).map(skin -> {
            // En edición no cambiamos el catálogo (mantener identidad de la publicación)
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

    /**
     * Resuelve el catálogo según la regla de negocio:
     * - Si requiereCatalogo=true (USER) → catalogoId obligatorio.
     * - Si requiereCatalogo=false (ADMIN) → opcional.
     */
    private SkinCatalogo resolverCatalogo(SkinRequest req, boolean requiereCatalogo) {
        if (req.getCatalogoId() == null) {
            if (requiereCatalogo) {
                throw new RuntimeException(
                        "Los usuarios solo pueden publicar skins basadas en el catálogo. " +
                        "Especificá un catalogoId válido en el request.");
            }
            return null; // admin sin catálogo: modo libre
        }
        return skinCatalogoRepository.findById(req.getCatalogoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Catálogo inexistente con id: " + req.getCatalogoId()));
    }

    private void aplicarCampos(Skin skin, SkinRequest req, Category category) {
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;
        // Si la skin tiene catálogo asociado, el name/description/game lo manda el catálogo
        if (skin.getCatalogo() != null) {
            skin.setName(skin.getCatalogo().getName());
            skin.setDescription(skin.getCatalogo().getDescription());
            skin.setGame("CS2");
        } else {
            skin.setName(req.getName());
            skin.setDescription(req.getDescription());
            skin.setGame(req.getGame());
        }
        skin.setPrice(req.getPrice());
        skin.setDiscount(discount);
        skin.setStock(req.getStock());
        skin.setCategory(category);

        if (req.getRareza() != null && !req.getRareza().isBlank()) {
            skin.setRareza(Skin.Rareza.valueOf(req.getRareza().toUpperCase()));
        }
        if (req.getExterior() != null && !req.getExterior().isBlank()) {
            skin.setExterior(Skin.Exterior.valueOf(req.getExterior().toUpperCase()));
        }
        if (req.getStattrak() != null) {
            skin.setStattrak(req.getStattrak());
        }
    }
}
