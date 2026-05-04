package skinsmarket.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import skinsmarket.demo.controller.inventario.PublicarDesdeInventarioRequest;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.InventarioItemRepository;
import skinsmarket.demo.repository.SkinCatalogoRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del servicio de inventario.
 *
 * Cliente de la API pública de Steam:
 *   GET https://steamcommunity.com/inventory/{steamId64}/730/2?l=english
 *
 * NO requiere API key. LIMITACIONES:
 *   - Inventario debe ser PÚBLICO en Steam (sino devuelve 403)
 *   - Rate limit por IP (~25 req/min). Devuelve 429.
 *   - SteamID inválido o cuenta sin CS2 → 400.
 *   - TRADE LOCK: items con tradable=0 no se pueden publicar.
 */
@Service
public class InventarioServiceImpl implements InventarioService {

    private static final String STEAM_INVENTORY_URL =
            "https://steamcommunity.com/inventory/{STEAM_ID}/730/2?l=english&count=5000";

    private static final String STEAM_IMG_BASE =
            "https://community.cloudflare.steamstatic.com/economy/image/";

    @Autowired
    private InventarioItemRepository inventarioItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkinCatalogoRepository skinCatalogoRepository;

    @Autowired
    private SkinRepository skinRepository;

    @Autowired
    private RestTemplate restTemplate;

    // =========================================================================
    // Sincronización con Steam
    // =========================================================================

    @Override
    @Transactional
    public int sincronizar(String email) {
        return doSync(email);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int sincronizarAislado(String email) {
        return doSync(email);
    }

    private int doSync(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getSteamId64() == null || user.getSteamId64().isBlank()) {
            throw new RuntimeException(
                    "Configurá tu SteamID64 en el perfil antes de sincronizar el inventario");
        }

        String json;
        try {
            String url = STEAM_INVENTORY_URL.replace("{STEAM_ID}", user.getSteamId64());
            json = restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException e) {
            // 4xx — error del cliente (lo que pedimos está mal)
            int status = e.getStatusCode().value();
            switch (status) {
                case 400:
                    throw new RuntimeException(
                            "Steam devolvió 400 (Bad Request). Causas más comunes: " +
                            "(1) SteamID64 incorrecto o inexistente; " +
                            "(2) la cuenta no tiene CS2 instalado; " +
                            "(3) el inventario nunca recibió items. " +
                            "Verificá el SteamID en https://steamid.io");
                case 401:
                case 403:
                    throw new RuntimeException(
                            "El inventario de este usuario es privado. " +
                            "Para que sea público en Steam: " +
                            "Perfil → Editar perfil → Privacidad → Inventario → Público. " +
                            "También 'Mi perfil' y 'Detalles del juego' deben estar en Público.");
                case 429:
                    throw new RuntimeException(
                            "Steam rate-limit alcanzado (HTTP 429). " +
                            "Esperá 5-10 minutos y volvé a intentar.");
                default:
                    throw new RuntimeException(
                            "Steam devolvió HTTP " + status + ". Verificá el SteamID64 " +
                            "y que el inventario sea público.");
            }
        } catch (HttpServerErrorException e) {
            // 5xx — error del servidor de Steam
            throw new RuntimeException(
                    "Steam tuvo un error interno (HTTP " + e.getStatusCode().value() + "). " +
                    "Probá de nuevo en unos minutos.");
        } catch (Exception e) {
            throw new RuntimeException("Error de red al contactar Steam: " + e.getMessage());
        }

        if (json == null || json.isBlank() || json.equals("null")) {
            throw new RuntimeException(
                    "Steam no devolvió datos. El inventario puede estar vacío " +
                    "o el SteamID puede ser inválido.");
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SteamInventoryResponse response;
        try {
            response = mapper.readValue(json, SteamInventoryResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear respuesta de Steam: " + e.getMessage());
        }

        if (response.getSuccess() != null && response.getSuccess() == 0) {
            throw new RuntimeException(
                    "Steam respondió success=0. Verificá que el inventario sea público " +
                    "y que no estés bajo rate-limit.");
        }

        if (response.getAssets() == null || response.getAssets().isEmpty()) {
            limpiarItemsNoPublicados(user, new ArrayList<>());
            return 0;
        }

        // Cruzar assets ↔ descriptions por (classid, instanceid)
        Map<String, SteamDescription> descByKey = new HashMap<>();
        if (response.getDescriptions() != null) {
            for (SteamDescription d : response.getDescriptions()) {
                descByKey.put(d.getClassid() + "_" + d.getInstanceid(), d);
            }
        }

        List<String> assetIdsActuales = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();

        for (SteamAsset asset : response.getAssets()) {
            String key = asset.getClassid() + "_" + asset.getInstanceid();
            SteamDescription desc = descByKey.get(key);
            if (desc == null) continue;

            assetIdsActuales.add(asset.getAssetid());

            Optional<InventarioItem> existente =
                    inventarioItemRepository.findByUserAndAssetId(user, asset.getAssetid());

            InventarioItem item = existente.orElseGet(() ->
                    InventarioItem.builder()
                            .user(user)
                            .assetId(asset.getAssetid())
                            .publicado(false)
                            .build());

            item.setClassId(asset.getClassid());
            item.setInstanceId(asset.getInstanceid());
            item.setMarketHashName(desc.getMarket_hash_name());
            item.setName(desc.getName());
            item.setIconUrl(desc.getIcon_url() != null ? STEAM_IMG_BASE + desc.getIcon_url() : null);
            item.setType(desc.getType());
            item.setTradable(desc.getTradable() != null && desc.getTradable() == 1);
            item.setMarketable(desc.getMarketable() != null && desc.getMarketable() == 1);
            item.setFechaSync(ahora);

            if (desc.getMarket_hash_name() != null) {
                List<SkinCatalogo> matches = skinCatalogoRepository
                        .findByNameContainingIgnoreCase(stripExterior(desc.getMarket_hash_name()));
                if (!matches.isEmpty()) {
                    item.setCatalogo(matches.get(0));
                }
            }

            inventarioItemRepository.save(item);
        }

        limpiarItemsNoPublicados(user, assetIdsActuales);
        return assetIdsActuales.size();
    }

    private void limpiarItemsNoPublicados(User user, List<String> assetIdsActuales) {
        List<InventarioItem> locales = inventarioItemRepository.findByUser(user);
        for (InventarioItem item : locales) {
            if (Boolean.TRUE.equals(item.getPublicado())) continue;
            if (!assetIdsActuales.contains(item.getAssetId())) {
                inventarioItemRepository.delete(item);
            }
        }
    }

    private String stripExterior(String fullName) {
        int idx = fullName.indexOf(" (");
        return idx > 0 ? fullName.substring(0, idx) : fullName;
    }

    // =========================================================================
    // Listado y publicación
    // =========================================================================

    @Override
    public List<InventarioItem> listarInventario(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return inventarioItemRepository.findByUser(user);
    }

    @Override
    @Transactional
    public Skin publicarDesdeInventario(String email, Long inventarioItemId,
                                        PublicarDesdeInventarioRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        InventarioItem item = inventarioItemRepository.findById(inventarioItemId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item de inventario no encontrado con id: " + inventarioItemId));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Este item no pertenece a tu inventario");
        }
        if (Boolean.TRUE.equals(item.getPublicado())) {
            throw new RuntimeException("Este item ya está publicado a la venta");
        }
        if (item.getCatalogo() == null) {
            throw new RuntimeException(
                    "Este item no tiene match con el catálogo. Sincronizá el catálogo " +
                    "(POST /catalogo/sincronizar?limit=500) y resincronizá tu inventario " +
                    "(POST /inventario/sync) para que se haga el match.");
        }
        if (Boolean.FALSE.equals(item.getTradable())) {
            throw new RuntimeException(
                    "Este item no es tradeable en Steam (trade lock activo o item " +
                    "intransferible por diseño, como medallas/coins). No se puede publicar.");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new RuntimeException("El precio debe ser mayor a 0");
        }

        SkinCatalogo cat = item.getCatalogo();
        Skin skin = new Skin();
        skin.setName(cat.getName());
        skin.setDescription(cat.getDescription());
        skin.setGame("CS2");
        skin.setPrice(request.getPrice());
        skin.setDiscount(request.getDiscount() != null ? request.getDiscount() : 0.0);
        skin.setStock(1);
        skin.setActive(true);
        skin.setStattrak(false);
        skin.setFechaAlta(LocalDateTime.now());
        skin.setVendedor(user);
        skin.setCatalogo(cat);
        skin.setImage(new byte[0]);

        Skin saved = skinRepository.save(skin);

        item.setPublicado(true);
        inventarioItemRepository.save(item);

        return saved;
    }

    // =========================================================================
    // DTOs internos para deserializar la respuesta de Steam
    // =========================================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamInventoryResponse {
        private List<SteamAsset> assets;
        private List<SteamDescription> descriptions;
        private Integer total_inventory_count;
        private Integer success;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamAsset {
        private String assetid;
        private String classid;
        private String instanceid;
        private String amount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamDescription {
        private String classid;
        private String instanceid;
        private String name;
        private String market_hash_name;
        private String market_name;
        private String icon_url;
        private String icon_url_large;
        private String type;
        private Integer tradable;
        private Integer marketable;
        private String name_color;
    }
}
