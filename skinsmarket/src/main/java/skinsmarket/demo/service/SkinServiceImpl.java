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
 * Estructura idéntica al GameServiceImpl del TPO aprobado:
 * mismas validaciones (InfoValidator), mismo patrón @Transactional,
 * mismo uso de @Autowired. Solo se adaptan los campos al dominio de skins.
 *
 * Diferencias respecto al GameServiceImpl:
 *   - Relación con Category es ManyToOne (no ManyToMany como Game)
 *   - Se agrega vendedor (User) al crear desde el contexto de usuario
 *   - deleteSkin hace baja lógica (active=false) en vez de borrar el registro
 */
@Service
public class SkinServiceImpl implements SkinService {

    // Inyección con @Autowired (consistente con el TPO aprobado)
    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Obtiene una skin por su ID.
     * Lanza IllegalArgumentException si no existe (la misma estrategia del TPO aprobado).
     */
    @Override
    public Skin getSkinById(Long id) {
        return skinRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + id));
    }

    /**
     * Crea una nueva skin desde el panel de administración.
     *
     * Valida stock, precio y descuento con InfoValidator (igual que el TPO aprobado).
     * La categoría se busca por ID y se valida que exista.
     * El campo vendedor queda null cuando es el admin quien crea la skin.
     *
     * @throws NegativeStockException   si stock < 0
     * @throws NegativePriceException   si precio <= 0
     * @throws InvalidDiscountException si descuento fuera de [0, 1]
     */
    @Override
    @Transactional
    public Skin createSkin(SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {

        // Validaciones de negocio con InfoValidator (igual que GameServiceImpl del TPO)
        if (!InfoValidator.isValidStock(skinRequest.getStock())) {
            throw new NegativeStockException();
        }
        if (!InfoValidator.isValidPrice(skinRequest.getPrice())) {
            throw new NegativePriceException();
        }

        // Si no se envía descuento, asumimos 0 (sin descuento)
        double discount = skinRequest.getDiscount() != null ? skinRequest.getDiscount() : 0.0;
        if (!InfoValidator.isValidDiscount(discount)) {
            throw new InvalidDiscountException();
        }

        // Buscar y validar la categoría asociada
        Category category = null;
        if (skinRequest.getCategoryId() != null) {
            category = categoryRepository.findById(skinRequest.getCategoryId().longValue())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Categoría inexistente: " + skinRequest.getCategoryId()));
        }

        // Construir y persistir la entidad Skin
        Skin skin = new Skin();
        skin.setName(skinRequest.getName());
        skin.setPrice(skinRequest.getPrice());
        skin.setDiscount(discount);
        skin.setStock(skinRequest.getStock());
        skin.setGame(skinRequest.getGame());
        skin.setImageUrl(skinRequest.getImageUrl());
        skin.setCategory(category);
        skin.setActive(true);
        skin.setFechaAlta(LocalDateTime.now());

        return skinRepository.save(skin);
    }

    /**
     * Edita una skin existente desde el panel de administración.
     *
     * Usa el patrón map().orElse(null) del TPO aprobado:
     * si la skin existe la actualiza, si no devuelve null (controller responde 404).
     *
     * @throws NegativeStockException   si stock < 0
     * @throws NegativePriceException   si precio <= 0
     * @throws InvalidDiscountException si descuento fuera de [0, 1]
     */
    @Override
    @Transactional
    public Skin editSkin(Long id, SkinRequest skinRequest)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {

        // Validaciones de negocio
        if (!InfoValidator.isValidStock(skinRequest.getStock())) {
            throw new NegativeStockException();
        }
        if (!InfoValidator.isValidPrice(skinRequest.getPrice())) {
            throw new NegativePriceException();
        }
        double discount = skinRequest.getDiscount() != null ? skinRequest.getDiscount() : 0.0;
        if (!InfoValidator.isValidDiscount(discount)) {
            throw new InvalidDiscountException();
        }

        // Buscar la nueva categoría si se proveyó
        Category category = null;
        if (skinRequest.getCategoryId() != null) {
            category = categoryRepository.findById(skinRequest.getCategoryId().longValue())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Categoría inexistente: " + skinRequest.getCategoryId()));
        }

        // Buscar la skin y actualizar sus campos (patrón idéntico a GameServiceImpl del TPO)
        final Category finalCategory = category;
        return skinRepository.findById(id).map(skin -> {
            skin.setName(skinRequest.getName());
            skin.setPrice(skinRequest.getPrice());
            skin.setDiscount(discount);
            skin.setStock(skinRequest.getStock());
            skin.setGame(skinRequest.getGame());
            skin.setImageUrl(skinRequest.getImageUrl());
            skin.setCategory(finalCategory);
            return skinRepository.save(skin);
        }).orElse(null);
    }

    /**
     * Baja lógica: pone active=false en lugar de borrar el registro.
     * Devuelve true si la skin existía y fue desactivada, false si el ID no existe.
     * Así el controller puede responder 404 en lugar de 204 cuando el ID es inválido.
     */
    @Override
    @Transactional
    public boolean deleteSkin(Long id) {
        return skinRepository.findById(id).map(skin -> {
            skin.setActive(false);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    /**
     * Devuelve todas las skins (activas e inactivas) para el panel de admin.
     * Equivalente a getAllGames() del TPO aprobado.
     */
    @Override
    public List<Skin> getAllSkins() {
        return skinRepository.findAll();
    }

    /**
     * Devuelve solo las skins activas con stock > 0 para el catálogo público.
     * Equivalente a getAllAvailableGames() del TPO aprobado.
     */
    @Override
    public List<Skin> getAllAvailableSkins() {
        return skinRepository.findByActiveTrue().stream()
                .filter(s -> s.getStock() > 0)
                .toList();
    }

    /**
     * Filtra skins por nombre de categoría.
     * Equivalente a getGamesByCategory() del TPO aprobado.
     */
    @Override
    public List<Skin> getSkinsByCategory(String categoryName) {
        return skinRepository.findByCategory_Name(categoryName);
    }

    /**
     * Filtra skins por rango de precio [min, max].
     * Equivalente a findByRangePrice() del TPO aprobado.
     */
    @Override
    public List<Skin> findByRangePrice(Double min, Double max) {
        return skinRepository.findByPriceBetween(min, max);
    }

    /**
     * Filtra skins con precio <= max.
     * Equivalente a findByPriceMax() del TPO aprobado.
     */
    @Override
    public List<Skin> findByPriceMax(Double max) {
        return skinRepository.findByPriceLessThanEqual(max);
    }

    /**
     * Filtra skins con precio >= min.
     * Equivalente a findByPriceMin() del TPO aprobado.
     */
    @Override
    public List<Skin> findByPriceMin(Double min) {
        return skinRepository.findByPriceGreaterThanEqual(min);
    }

    /**
     * Busca skins cuyo nombre contenga el texto (case-insensitive).
     * Equivalente a findByTitle() del TPO aprobado.
     */
    @Override
    public List<Skin> findByName(String name) {
        return skinRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Devuelve todas las skins publicadas por el usuario (vendedor) identificado por email.
     */
    @Override
    public List<Skin> getSkinsByOwner(String email) {
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return skinRepository.findByVendedor(vendedor);
    }

    /**
     * Crea una skin con el usuario autenticado como vendedor.
     * Cualquier USER puede publicar su propio producto (requisito del TPO).
     */
    @Override
    @Transactional
    public Skin createSkinAsVendedor(SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {

        if (!InfoValidator.isValidStock(skinRequest.getStock()))  throw new NegativeStockException();
        if (!InfoValidator.isValidPrice(skinRequest.getPrice()))  throw new NegativePriceException();
        double discount = skinRequest.getDiscount() != null ? skinRequest.getDiscount() : 0.0;
        if (!InfoValidator.isValidDiscount(discount)) throw new InvalidDiscountException();

        Category category = null;
        if (skinRequest.getCategoryId() != null) {
            category = categoryRepository.findById(skinRequest.getCategoryId().longValue())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Categoría inexistente: " + skinRequest.getCategoryId()));
        }

        // El vendedor es el usuario autenticado (no el admin)
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));

        Skin skin = new Skin();
        skin.setName(skinRequest.getName());
        skin.setPrice(skinRequest.getPrice());
        skin.setDiscount(discount);
        skin.setStock(skinRequest.getStock());
        skin.setGame(skinRequest.getGame());
        skin.setImageUrl(skinRequest.getImageUrl());
        skin.setCategory(category);
        skin.setActive(true);
        skin.setFechaAlta(java.time.LocalDateTime.now());
        skin.setVendedor(vendedor); // asignamos el dueño de la publicación

        return skinRepository.save(skin);
    }

    /**
     * Edita una skin solo si pertenece al usuario autenticado.
     * Lanza 403 si otro usuario intenta editarla.
     */
    @Override
    @Transactional
    public Skin editSkinAsVendedor(Long id, SkinRequest skinRequest, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {

        if (!InfoValidator.isValidStock(skinRequest.getStock()))  throw new NegativeStockException();
        if (!InfoValidator.isValidPrice(skinRequest.getPrice()))  throw new NegativePriceException();
        double discount = skinRequest.getDiscount() != null ? skinRequest.getDiscount() : 0.0;
        if (!InfoValidator.isValidDiscount(discount)) throw new InvalidDiscountException();

        Category category = null;
        if (skinRequest.getCategoryId() != null) {
            final Long catId = skinRequest.getCategoryId().longValue();
            category = categoryRepository.findById(catId)
                    .orElseThrow(() -> new IllegalArgumentException("Categoría inexistente: " + catId));
        }

        final Category finalCategory = category;
        return skinRepository.findById(id).map(skin -> {
            // Verificar que el usuario autenticado sea el vendedor de la skin
            if (skin.getVendedor() == null || !skin.getVendedor().getEmail().equals(email)) {
                throw new RuntimeException("No tenés permiso para editar esta skin");
            }
            skin.setName(skinRequest.getName());
            skin.setPrice(skinRequest.getPrice());
            skin.setDiscount(discount);
            skin.setStock(skinRequest.getStock());
            skin.setGame(skinRequest.getGame());
            skin.setImageUrl(skinRequest.getImageUrl());
            skin.setCategory(finalCategory);
            return skinRepository.save(skin);
        }).orElse(null);
    }

    /**
     * Baja lógica de una skin solo si le pertenece al usuario autenticado.
     */
    @Override
    @Transactional
    public boolean deleteSkinAsVendedor(Long id, String email) {
        return skinRepository.findById(id).map(skin -> {
            if (skin.getVendedor() == null || !skin.getVendedor().getEmail().equals(email)) {
                throw new RuntimeException("No tenés permiso para eliminar esta skin");
            }
            skin.setActive(false);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }
}