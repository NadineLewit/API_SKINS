package skinsmarket.demo.service;

import skinsmarket.demo.controller.skin.SkinRequest;
import skinsmarket.demo.entity.Carrito;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.ItemCarrito;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.exception.InvalidDiscountException;
import skinsmarket.demo.exception.NegativePriceException;
import skinsmarket.demo.exception.NegativeStockException;
import skinsmarket.demo.repository.CarritoRepository;
import skinsmarket.demo.repository.InventarioItemRepository;
import skinsmarket.demo.repository.ItemCarritoRepository;
import skinsmarket.demo.repository.SkinCatalogoRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;
import skinsmarket.demo.utils.InfoValidator;
import skinsmarket.demo.utils.TradeProfileValidator;
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

    private static final int STOCK_DISPONIBLE_MINIMO = 0;

    @Autowired private SkinRepository skinRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SkinCatalogoRepository skinCatalogoRepository;
    @Autowired private ItemCarritoRepository itemCarritoRepository;
    @Autowired private CarritoRepository carritoRepository;
    @Autowired private InventarioItemRepository inventarioItemRepository;
    @Autowired private SteamMarketPriceService steamMarketPriceService;

    @Override
    public Skin getSkinById(Long id) {
        return withEstimatedTradePrice(skinRepository.findPublicadaDisponibleById(
                        id, STOCK_DISPONIBLE_MINIMO)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Skin no encontrada o no disponible: " + id)));
    }

    @Override
    @Transactional
    public Skin createSkin(SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        return buildAndSaveSkin(req, null);
    }

    @Override
    @Transactional
    public Skin createSkinForUser(String vendedorEmail, SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        String email = InfoValidator.normalizeEmail(vendedorEmail);
        if (!InfoValidator.isValidEmail(email)) {
            throw new IllegalArgumentException("Email de vendedor inválido");
        }

        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un usuario con email: " + email));

        return buildAndSaveSkin(req, vendedor);
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
            if (noEsPausable(skin)) {
                throw new RuntimeException("No se puede eliminar una publicación vendida, reservada o ya eliminada");
            }
            quitarDeCarritos(skin);
            liberarInventarioItem(skin);
            skin.setActive(false);
            skin.setStock(0);
            skin.setEstadoPublicacion(Skin.EstadoPublicacion.ELIMINADA_ADMIN);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean activateSkin(Long id) {
        return skinRepository.findById(id).map(skin -> {
            if (skin.getEstadoPublicacion() == Skin.EstadoPublicacion.VENDIDA ||
                    skin.getEstadoPublicacion() == Skin.EstadoPublicacion.RESERVADA ||
                    skin.getEstadoPublicacion() == Skin.EstadoPublicacion.ELIMINADA_ADMIN) {
                throw new RuntimeException("No se puede activar una publicación vendida, reservada o eliminada por admin");
            }
            validarPerfilVendedorSiExiste(skin);
            skin.setActive(true);
            skin.setStock(1);
            skin.setEstadoPublicacion(Skin.EstadoPublicacion.PUBLICADA);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    @Override
    public List<Skin> getAllSkins(boolean includeInactive) {
        if (includeInactive) return withEstimatedTradePrices(skinRepository.findAll());
        return withEstimatedTradePrices(skinRepository.findByActiveTrue());
    }

    @Override
    public List<Skin> getAllAvailableSkins(Boolean intercambiable, Boolean vendible) {
        return withEstimatedTradePrices(aplicarFiltrosOperacion(
                skinRepository.findPublicadasDisponibles(STOCK_DISPONIBLE_MINIMO),
                intercambiable,
                vendible));
    }

    @Override
    public List<Skin> getSkinsByCategoryName(String categoryName) {
        return withEstimatedTradePrices(skinRepository
                .findPublicadasDisponiblesByCategoryName(
                        categoryName, STOCK_DISPONIBLE_MINIMO));
    }

    @Override
    public List<Skin> findByRangePrice(Double min, Double max) {
        return withEstimatedTradePrices(skinRepository.findPublicadasDisponiblesByPriceBetween(
                min, max, STOCK_DISPONIBLE_MINIMO));
    }

    @Override
    public List<Skin> findByPriceMax(Double max) {
        return withEstimatedTradePrices(skinRepository.findPublicadasDisponiblesByPriceLessThanEqual(
                max, STOCK_DISPONIBLE_MINIMO));
    }

    @Override
    public List<Skin> findByPriceMin(Double min) {
        return withEstimatedTradePrices(skinRepository.findPublicadasDisponiblesByPriceGreaterThanEqual(
                min, STOCK_DISPONIBLE_MINIMO));
    }

    @Override
    public List<Skin> findByName(String name) {
        return withEstimatedTradePrices(skinRepository.findPublicadasDisponiblesByName(
                name, STOCK_DISPONIBLE_MINIMO));
    }

    @Override
    public List<Skin> getSkinsByOwner(String email) {
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return withEstimatedTradePrices(skinRepository.findByVendedor(vendedor).stream()
                .filter(s -> {
                    Skin.EstadoPublicacion estado = estadoNormalizado(s);
                    return estado == Skin.EstadoPublicacion.PUBLICADA ||
                            (estado == Skin.EstadoPublicacion.PAUSADA &&
                                    s.getStock() != null && s.getStock() > 0);
                })
                .toList());
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
            if (skin.getEstadoPublicacion() == Skin.EstadoPublicacion.ELIMINADA_ADMIN) {
                throw new RuntimeException("No se puede editar una publicación eliminada por admin");
            }
            aplicarCampos(skin, req);
            return withEstimatedTradePrice(skinRepository.save(skin));
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
            if (noEsPausable(skin)) {
                throw new RuntimeException("No se puede retirar una publicación vendida, reservada o eliminada por admin");
            }
            quitarDeCarritos(skin);
            liberarInventarioItem(skin);
            skin.setActive(false);
            skin.setStock(0);
            skin.setEstadoPublicacion(Skin.EstadoPublicacion.PAUSADA);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean pauseSkinAsVendedor(Long id, String email) {
        return skinRepository.findById(id).map(skin -> {
            User usuario = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            boolean esAdmin = usuario.getRole().name().equals("ADMIN");
            boolean esVendedor = skin.getVendedor() != null
                    && skin.getVendedor().getEmail().equals(email);
            if (!esAdmin && !esVendedor) {
                throw new RuntimeException("No tenés permiso para pausar esta skin");
            }
            if (noEsPausable(skin)) {
                throw new RuntimeException("No se puede pausar una publicación vendida, reservada o eliminada");
            }
            quitarDeCarritos(skin);
            skin.setActive(false);
            skin.setStock(1);
            skin.setEstadoPublicacion(Skin.EstadoPublicacion.PAUSADA);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional
    public boolean activateSkinAsVendedor(Long id, String email) {
        return skinRepository.findById(id).map(skin -> {
            User usuario = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
            boolean esAdmin = usuario.getRole().name().equals("ADMIN");
            boolean esVendedor = skin.getVendedor() != null
                    && skin.getVendedor().getEmail().equals(email);
            if (!esAdmin && !esVendedor) {
                throw new RuntimeException("No tenés permiso para activar esta skin");
            }
            if (skin.getEstadoPublicacion() == Skin.EstadoPublicacion.VENDIDA ||
                    skin.getEstadoPublicacion() == Skin.EstadoPublicacion.RESERVADA ||
                    skin.getEstadoPublicacion() == Skin.EstadoPublicacion.ELIMINADA_ADMIN) {
                throw new RuntimeException("No se puede activar una publicación vendida, reservada o eliminada por admin");
            }
            validarPerfilVendedorSiExiste(skin);
            InventarioItem item = skin.getInventarioItem();
            if (item == null && skin.getSteamAssetId() != null && !skin.getSteamAssetId().isBlank()) {
                throw new RuntimeException("Esta skin fue devuelta al inventario. Volvé a publicarla desde Inventario.");
            }
            if (item != null && !Boolean.TRUE.equals(item.getPublicado())) {
                item.setPublicado(true);
                inventarioItemRepository.save(item);
            }
            skin.setActive(true);
            skin.setStock(1);
            skin.setEstadoPublicacion(Skin.EstadoPublicacion.PUBLICADA);
            skinRepository.save(skin);
            return true;
        }).orElse(false);
    }

    @Override
    public List<Skin> getHistorialSkinsByOwner(String email) {
        User vendedor = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + email));
        return withEstimatedTradePrices(skinRepository.findByVendedor(vendedor).stream()
                .filter(s -> {
                    Skin.EstadoPublicacion estado = estadoNormalizado(s);
                    return estado == Skin.EstadoPublicacion.RESERVADA ||
                            estado == Skin.EstadoPublicacion.VENDIDA ||
                            estado == Skin.EstadoPublicacion.ELIMINADA_ADMIN;
                })
                .toList());
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private Skin buildAndSaveSkin(SkinRequest req, User vendedor)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(req);
        if (vendedor != null) {
            TradeProfileValidator.requireTradeUrl(vendedor, "vender");
            TradeProfileValidator.requireAliasCobro(vendedor, "vender");
        }
        SkinCatalogo catalogo = resolverCatalogo(req);

        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;

        Skin skin = new Skin();
        skin.setPrice(req.getPrice());
        skin.setDiscount(discount);
        skin.setStock(1);
        aplicarDisponibilidad(skin, req.getIntercambiable(), req.getVendible());
        skin.setActive(true);
        skin.setEstadoPublicacion(Skin.EstadoPublicacion.PUBLICADA);
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

        return withEstimatedTradePrice(skinRepository.save(skin));
    }

    private void validarPerfilVendedorSiExiste(Skin skin) {
        if (skin.getVendedor() == null) return;
        TradeProfileValidator.requireTradeUrl(skin.getVendedor(), "activar publicaciones");
    }

    private Skin updateSkin(Long id, SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
        validarDatos(req);

        return skinRepository.findById(id).map(skin -> {
            aplicarCampos(skin, req);
            return withEstimatedTradePrice(skinRepository.save(skin));
        }).orElse(null);
    }

    private void validarDatos(SkinRequest req)
            throws NegativeStockException, NegativePriceException, InvalidDiscountException {
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
        if (req.getIntercambiable() != null) {
            skin.setIntercambiable(req.getIntercambiable());
        }
        if (req.getVendible() != null) {
            skin.setVendible(req.getVendible());
        }
        validarDisponibilidadExclusiva(skin.getIntercambiable(), skin.getVendible());

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

    private void aplicarDisponibilidad(Skin skin, Boolean intercambiable, Boolean vendible) {
        boolean vendibleValue = vendible == null || Boolean.TRUE.equals(vendible);
        boolean intercambiableValue = Boolean.TRUE.equals(intercambiable);
        validarDisponibilidadExclusiva(intercambiableValue, vendibleValue);
        skin.setIntercambiable(intercambiableValue);
        skin.setVendible(vendibleValue);
    }

    private void validarDisponibilidadExclusiva(Boolean intercambiable, Boolean vendible) {
        boolean esIntercambiable = Boolean.TRUE.equals(intercambiable);
        boolean esVendible = Boolean.TRUE.equals(vendible);
        if (esIntercambiable == esVendible) {
            throw new RuntimeException("La skin debe ser vendible o intercambiable, no ambas.");
        }
    }

    private boolean noEsPausable(Skin skin) {
        return skin.getEstadoPublicacion() == Skin.EstadoPublicacion.VENDIDA ||
                skin.getEstadoPublicacion() == Skin.EstadoPublicacion.RESERVADA ||
                skin.getEstadoPublicacion() == Skin.EstadoPublicacion.ELIMINADA_ADMIN ||
                skin.getStock() == null || skin.getStock() < 1;
    }

    private void quitarDeCarritos(Skin skin) {
        List<ItemCarrito> items = itemCarritoRepository.findBySkin(skin);
        for (ItemCarrito item : items) {
            Carrito carrito = item.getCarrito();
            if (carrito == null) {
                itemCarritoRepository.delete(item);
                continue;
            }
            carrito.quitarItem(item);
            carritoRepository.save(carrito);
        }
    }

    private void liberarInventarioItem(Skin skin) {
        InventarioItem item = skin.getInventarioItem();
        if (item == null) return;
        item.setPublicado(false);
        item.setInventoryStatus(InventarioItem.STATUS_STEAM);
        item.setPendingOrderId(null);
        item.setPendingSkinId(null);
        item.setPendingUntil(null);
        item.setDeliveredAt(LocalDateTime.now());
        inventarioItemRepository.save(item);
        skin.setInventarioItem(null);
    }

    private Skin.EstadoPublicacion estadoNormalizado(Skin skin) {
        if (skin.getEstadoPublicacion() != null) return skin.getEstadoPublicacion();
        Skin.EstadoPublicacion estado;
        if (!Boolean.TRUE.equals(skin.getActive())) {
            estado = Skin.EstadoPublicacion.PAUSADA;
        } else if (skin.getStock() == null || skin.getStock() < 1) {
            estado = Skin.EstadoPublicacion.VENDIDA;
        } else {
            estado = Skin.EstadoPublicacion.PUBLICADA;
        }
        skin.setEstadoPublicacion(estado);
        return estado;
    }

    private List<Skin> withEstimatedTradePrices(List<Skin> skins) {
        skins.forEach(this::withEstimatedTradePrice);
        return skins;
    }

    private Skin withEstimatedTradePrice(Skin skin) {
        if (skin == null) return null;
        double fallback = skin.getFinalPrice() != null ? skin.getFinalPrice() : skin.getPrice();
        if (fallback > 1.01) {
            skin.setEstimatedTradePrice(fallback);
            return skin;
        }
        skin.setEstimatedTradePrice(steamMarketPriceService.estimateSkinPriceUsd(skin, fallback));
        return skin;
    }

    private List<Skin> aplicarFiltrosOperacion(
            List<Skin> skins, Boolean intercambiable, Boolean vendible) {
        return skins.stream()
                .filter(s -> intercambiable == null ||
                        (Boolean.TRUE.equals(s.getIntercambiable()) &&
                                !Boolean.TRUE.equals(s.getVendible())) == intercambiable)
                .filter(s -> vendible == null || Boolean.TRUE.equals(s.getVendible()) == vendible)
                .toList();
    }

}
