package skinsmarket.demo.service;

import skinsmarket.demo.controller.skin.SkinRequest;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;
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
 * SkinServiceImpl SIN Category y SIN BLOB.
 *
 * Las imágenes vienen del catálogo (catalogo.imageUrl).
 * Las categorías vienen del catálogo (catalogo.categoryName).
 */
@Service
public class SkinServiceImpl implements SkinService {

    @Autowired private SkinRepository skinRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SkinCatalogoRepository skinCatalogoRepository;

    @Override
    public Skin getSkinById(Long id) {
        return skinRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Skin no encontrada: " + id));
    }

    @Override
    @Transactional
    public Skin createSkin(SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return buildAndSaveSkin(req, null);
    }

    @Override
    @Transactional
    public Skin editSkin(Long id, SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return updateSkin(id, req);
    }

    @Override
    @Transactional
    public boolean deleteSkin(Long id) {
        return skinRepository.findById(id).map(skin -> {
            skin.setActive(false);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    @Override
    public List<Skin> getAllSkins(boolean includeInactive) {
        if (includeInactive) return skinRepository.findAll();
        return skinRepository.findByActiveTrue();
    }

    @Override
    public List<Skin> getAllAvailableSkins() {
        return skinRepository.findByActiveTrue().stream()
                .filter(s -> s.getStock() > 0).toList();
    }

    @Override
    public List<Skin> getSkinsByCategoryName(String categoryName) {
        return skinRepository.findByCatalogo_CategoryNameContainingIgnoreCase(categoryName);
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

    @Override
    @Transactional
    public Skin editSkinAsVendedor(Long id, SkinRequest req, String email)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(req);

        return skinRepository.findById(id).map(skin -> {
            User usuario = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            boolean esAdmin = usuario.getRole().name().equals("ADMIN");
            boolean esVendedor = skin.getVendedor() != null
                    && skin.getVendedor().getEmail().equals(email);
            if (!esAdmin && !esVendedor) {
                throw new RuntimeException("No tenés permiso para editar esta skin");
            }
            aplicarCampos(skin, req);
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

    private Skin buildAndSaveSkin(SkinRequest req, User vendedor)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(req);
        SkinCatalogo catalogo = resolverCatalogo(req);

        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;

        Skin skin = new Skin();
        skin.setPrice(req.getPrice());
        skin.setDiscount(discount);
        skin.setStock(req.getStock());
        skin.setActive(true);
        skin.setFechaAlta(LocalDateTime.now());
        skin.setVendedor(vendedor);
        skin.setCatalogo(catalogo);

        if (catalogo != null) {
            skin.setName(catalogo.getName());
            skin.setDescription(catalogo.getDescription());
            skin.setGame("CS2");
            skin.setImageUrl(catalogo.getImageUrl());
        } else {
            skin.setName(req.getName());
            skin.setDescription(req.getDescription());
            skin.setGame(req.getGame());
            skin.setImageUrl(req.getImageUrl());
        }

        if (req.getRareza() != null && !req.getRareza().isBlank()) {
            skin.setRareza(Skin.Rareza.valueOf(req.getRareza().toUpperCase()));
        }
        if (req.getExterior() != null && !req.getExterior().isBlank()) {
            skin.setExterior(Skin.Exterior.valueOf(req.getExterior().toUpperCase()));
        }
        skin.setStattrak(Boolean.TRUE.equals(req.getStattrak()));

        return skinRepository.save(skin);
    }

    private Skin updateSkin(Long id, SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(req);

        return skinRepository.findById(id).map(skin -> {
            aplicarCampos(skin, req);
            return skinRepository.save(skin);
        }).orElse(null);
    }

    private void validarDatos(SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        if (!InfoValidator.isValidStock(req.getStock())) throw new NegativeStockException();
        if (!InfoValidator.isValidPrice(req.getPrice())) throw new NegativePriceException();
        double d = req.getDiscount() != null ? req.getDiscount() : 0.0;
        if (!InfoValidator.isValidDiscount(d)) throw new InvalidDiscountException();
    }

    private SkinCatalogo resolverCatalogo(SkinRequest req) {
        if (req.getCatalogoId() == null) return null;
        return skinCatalogoRepository.findById(req.getCatalogoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Catálogo inexistente con id: " + req.getCatalogoId()));
    }

    private void aplicarCampos(Skin skin, SkinRequest req) {
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;

        if (skin.getCatalogo() != null) {
            skin.setName(skin.getCatalogo().getName());
            skin.setDescription(skin.getCatalogo().getDescription());
            skin.setGame("CS2");
            skin.setImageUrl(skin.getCatalogo().getImageUrl());
        } else {
            if (req.getName() != null)        skin.setName(req.getName());
            if (req.getDescription() != null) skin.setDescription(req.getDescription());
            if (req.getGame() != null)        skin.setGame(req.getGame());
            if (req.getImageUrl() != null)    skin.setImageUrl(req.getImageUrl());
        }

        skin.setPrice(req.getPrice());
        skin.setDiscount(discount);
        skin.setStock(req.getStock());

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
