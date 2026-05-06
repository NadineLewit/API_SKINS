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
 * VERSIÓN CON LOGS DETALLADOS para diagnosticar por qué el inventario está vacío.
 * Cada paso del sync imprime en consola lo que está pasando:
 *   - Si Steam responde
 *   - Cuántos assets devuelve
 *   - Cuántos descriptions devuelve
 *   - Cuántos se persisten finalmente
 *
 * Cliente de la API pública de Steam:
 *   GET https://steamcommunity.com/inventory/{steamId64}/730/2
 */
@Service
public class InventarioServiceImpl implements InventarioService {

    private static final String STEAM_INVENTORY_URL =
            "https://steamcommunity.com/inventory/{STEAM_ID}/730/2?l=english&count=2000";

    private static final String STEAM_IMG_BASE =
            "https://community.cloudflare.steamstatic.com/economy/image/";

    @Autowired private InventarioItemRepository inventarioItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SkinCatalogoRepository skinCatalogoRepository;
    @Autowired private SkinRepository skinRepository;
    @Autowired private RestTemplate restTemplate;

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
        System.out.println("══════════════════════════════════════════════════════════");
        System.out.println("[SYNC] Iniciando sincronización para: " + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));

        System.out.println("[SYNC] User encontrado. ID=" + user.getId() +
                ", steamId64=" + user.getSteamId64());

        if (user.getSteamId64() == null || user.getSteamId64().isBlank()) {
            throw new RuntimeException(
                    "Configurá tu SteamID64 en el perfil antes de sincronizar el inventario");
        }

        // 1. Pedir inventario a Steam
        String json = pedirInventarioASteam(user.getSteamId64());

        // 2. Parsear
        SteamInventoryResponse response = parsearJson(json);

        System.out.println("[SYNC] Respuesta parseada. " +
                "success=" + response.getSuccess() +
                ", assets=" + (response.getAssets() == null ? "null" : response.getAssets().size()) +
                ", descriptions=" + (response.getDescriptions() == null ? "null" : response.getDescriptions().size()) +
                ", total_inventory_count=" + response.getTotal_inventory_count());

        if (response.getSuccess() != null && response.getSuccess() == 0) {
            throw new RuntimeException(
                    "Steam respondió success=0. Verificá que el inventario sea público " +
                    "y que no estés bajo rate-limit.");
        }

        if (response.getAssets() == null || response.getAssets().isEmpty()) {
            System.out.println("[SYNC] Inventario VACÍO en Steam. Limpiando items locales.");
            limpiarItemsNoPublicados(user, new ArrayList<>());
            return 0;
        }

        // 3. Indexar descriptions
        Map<String, SteamDescription> descByKey = new HashMap<>();
        if (response.getDescriptions() != null) {
            for (SteamDescription d : response.getDescriptions()) {
                descByKey.put(d.getClassid() + "_" + d.getInstanceid(), d);
            }
        }
        System.out.println("[SYNC] Descriptions indexadas: " + descByKey.size());

        // 4. Upsert
        List<String> assetIdsActuales = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now();
        int saltadosSinDesc = 0;
        int saltadosError = 0;
        int procesados = 0;

        for (SteamAsset asset : response.getAssets()) {
            String key = asset.getClassid() + "_" + asset.getInstanceid();
            SteamDescription desc = descByKey.get(key);
            if (desc == null) {
                saltadosSinDesc++;
                System.out.println("[SYNC] ⚠ Asset " + asset.getAssetid() +
                        " sin description (key=" + key + "). Salteado.");
                continue;
            }

            try {
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

                String iconUrl = desc.getIcon_url() != null
                        ? STEAM_IMG_BASE + desc.getIcon_url()
                        : null;
                item.setIconUrl(iconUrl);

                item.setType(desc.getType());
                item.setTradable(desc.getTradable() != null && desc.getTradable() == 1);
                item.setMarketable(desc.getMarketable() != null && desc.getMarketable() == 1);
                item.setFechaSync(ahora);

                SkinCatalogo catalogo = matchearConCatalogo(desc.getMarket_hash_name());
                item.setCatalogo(catalogo);

                inventarioItemRepository.save(item);
                procesados++;
            } catch (Exception e) {
                saltadosError++;
                System.err.println("[SYNC] ✗ Error procesando asset " +
                        asset.getAssetid() + ": " + e.getMessage());
            }
        }

        System.out.println("[SYNC] ✓ Procesados: " + procesados +
                " | Saltados sin desc: " + saltadosSinDesc +
                " | Errores: " + saltadosError);

        // 5. Limpiar items que ya no están
        limpiarItemsNoPublicados(user, assetIdsActuales);

        System.out.println("[SYNC] ✓ Sync terminado. Items en BD: " + procesados);
        System.out.println("══════════════════════════════════════════════════════════");

        return procesados;
    }

    private SkinCatalogo matchearConCatalogo(String marketHashName) {
        if (marketHashName == null || marketHashName.isBlank()) return null;

        Optional<SkinCatalogo> exacto = skinCatalogoRepository.findByName(marketHashName);
        if (exacto.isPresent()) return exacto.get();

        String sinExterior = stripExterior(marketHashName);
        if (!sinExterior.equals(marketHashName)) {
            exacto = skinCatalogoRepository.findByName(sinExterior);
            if (exacto.isPresent()) return exacto.get();
        }

        List<SkinCatalogo> parcial = skinCatalogoRepository
                .findByNameContainingIgnoreCase(sinExterior);
        return parcial.isEmpty() ? null : parcial.get(0);
    }

    private String stripExterior(String fullName) {
        int idx = fullName.indexOf(" (");
        return idx > 0 ? fullName.substring(0, idx) : fullName;
    }

    private void limpiarItemsNoPublicados(User user, List<String> assetIdsActuales) {
        List<InventarioItem> locales = inventarioItemRepository.findByUser(user);
        int borrados = 0;
        for (InventarioItem item : locales) {
            if (Boolean.TRUE.equals(item.getPublicado())) continue;
            if (!assetIdsActuales.contains(item.getAssetId())) {
                inventarioItemRepository.delete(item);
                borrados++;
            }
        }
        if (borrados > 0) {
            System.out.println("[SYNC] Items locales borrados (no estaban en Steam): " + borrados);
        }
    }

    private String pedirInventarioASteam(String steamId64) {
        String url = STEAM_INVENTORY_URL.replace("{STEAM_ID}", steamId64);
        System.out.println("[SYNC] Llamando a: " + url);

        try {
            String json = restTemplate.getForObject(url, String.class);
            int len = json == null ? 0 : json.length();
            System.out.println("[SYNC] Respuesta de Steam recibida. Bytes: " + len);
            if (len > 0 && len < 200) {
                System.out.println("[SYNC] Respuesta corta (sospechoso): " + json);
            }
            if (json == null || json.isBlank() || json.equals("null")) {
                throw new RuntimeException(
                        "Steam no devolvió datos. El inventario puede estar vacío " +
                        "o el SteamID puede ser inválido.");
            }
            return json;
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            System.err.println("[SYNC] Steam respondió HTTP " + status);
            switch (status) {
                case 400:
                    throw new RuntimeException(
                            "Steam devolvió 400. SteamID inválido, sin CS2 o inventario inexistente.");
                case 401:
                case 403:
                    throw new RuntimeException(
                            "El inventario es privado. En Steam: Perfil → Editar perfil → " +
                            "Privacidad → Inventario, Mi perfil y Detalles del juego en Público.");
                case 429:
                    throw new RuntimeException(
                            "Steam rate-limit (HTTP 429). Esperá 5-10 minutos.");
                default:
                    throw new RuntimeException(
                            "Steam devolvió HTTP " + status);
            }
        } catch (HttpServerErrorException e) {
            throw new RuntimeException(
                    "Steam tuvo error interno (HTTP " + e.getStatusCode().value() + ").");
        } catch (Exception e) {
            System.err.println("[SYNC] Excepción de red: " + e.getClass().getName() +
                    " - " + e.getMessage());
            throw new RuntimeException("Error de red al contactar Steam: " + e.getMessage());
        }
    }

    private SteamInventoryResponse parsearJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return mapper.readValue(json, SteamInventoryResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al parsear respuesta de Steam: " + e.getMessage());
        }
    }

    @Override
    public List<InventarioItem> listarInventario(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
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
                    "Este item NO ES PUBLICABLE. Solo skins de armas reconocidas " +
                    "en el catálogo se pueden publicar.");
        }
        if (Boolean.FALSE.equals(item.getTradable())) {
            throw new RuntimeException(
                    "Este item no es tradeable en Steam (trade lock o intransferible).");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new RuntimeException("El precio debe ser mayor a 0");
        }

        SkinCatalogo cat = item.getCatalogo();
        Skin skin = new Skin();
        skin.setName(cat.getName());
        skin.setDescription(cat.getDescription());
        skin.setGame("CS2");
        skin.setImageUrl(cat.getImageUrl());
        skin.setPrice(request.getPrice());
        skin.setDiscount(request.getDiscount() != null ? request.getDiscount() : 0.0);
        skin.setStock(1);
        skin.setActive(true);
        skin.setStattrak(false);
        skin.setFechaAlta(LocalDateTime.now());
        skin.setVendedor(user);
        skin.setCatalogo(cat);

        Skin saved = skinRepository.save(skin);

        item.setPublicado(true);
        inventarioItemRepository.save(item);

        return saved;
    }

    // DTOs internos
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
