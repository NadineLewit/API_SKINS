package skinsmarket.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.SkinCatalogo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SteamMarketPriceService {

    private static final Duration FAILED_FETCH_BACKOFF = Duration.ofMinutes(5);
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    @Autowired
    private RestTemplate restTemplate;

    @Value("${steam.market.discount-rate:0.08}")
    private double discountRate;

    @Value("${steam.market.price-feed-url:https://raw.githubusercontent.com/SKINSTRACK/CS2-Price-API/main/free_prices.json}")
    private String priceFeedUrl;

    @Value("${steam.market.price-feed-cache-minutes:360}")
    private long priceFeedCacheMinutes;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object priceFeedLock = new Object();
    private volatile Map<String, Double> priceFeedCache = Collections.emptyMap();
    private volatile Instant priceFeedFetchedAt = Instant.EPOCH;
    private volatile Instant lastFailedFetchAt = Instant.EPOCH;

    public double estimateSkinPriceUsd(Skin skin, double fallbackUsd) {
        if (skin == null) return roundMoney(fallbackUsd);
        return estimatePriceUsd(resolveMarketHashName(skin), fallbackUsd);
    }

    public double estimateInventoryItemPriceUsd(InventarioItem item, double fallbackUsd) {
        if (item == null) return roundMoney(fallbackUsd);
        return findPriceUsd(resolveMarketHashNames(item))
                .orElseGet(() -> roundMoney(fallbackUsd));
    }

    public Optional<Double> findInventoryItemPriceUsd(InventarioItem item) {
        if (item == null) return Optional.empty();
        return findPriceUsd(resolveMarketHashNames(item));
    }

    public Optional<Double> findSkinPriceUsd(Skin skin) {
        if (skin == null) return Optional.empty();
        return findPriceUsd(resolveMarketHashName(skin));
    }

    public double estimatePriceUsd(String marketHashName, double fallbackUsd) {
        return findPriceUsd(marketHashName)
                .orElseGet(() -> roundMoney(fallbackUsd));
    }

    public Optional<Double> findPriceUsd(String marketHashName) {
        if (marketHashName == null || marketHashName.isBlank()) {
            return Optional.empty();
        }
        LinkedHashSet<String> single = new LinkedHashSet<>();
        single.add(marketHashName);
        return findPriceUsd(single);
    }

    private Optional<Double> findPriceUsd(Set<String> marketHashNames) {
        if (marketHashNames == null || marketHashNames.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Double> prices = loadPriceFeed();
        if (prices.isEmpty()) {
            return Optional.empty();
        }

        for (String marketHashName : marketHashNames) {
            for (String candidate : marketHashCandidates(marketHashName)) {
                Double priceUsd = prices.get(normalizePriceKey(candidate));
                if (priceUsd != null && priceUsd > 0) {
                    return Optional.of(roundMoney(applyDiscount(priceUsd)));
                }
            }
        }
        return Optional.empty();
    }

    private Map<String, Double> loadPriceFeed() {
        Instant now = Instant.now();
        Duration ttl = Duration.ofMinutes(Math.max(priceFeedCacheMinutes, 1));

        if (!priceFeedCache.isEmpty() && priceFeedFetchedAt.plus(ttl).isAfter(now)) {
            return priceFeedCache;
        }
        if (priceFeedCache.isEmpty() && lastFailedFetchAt.plus(FAILED_FETCH_BACKOFF).isAfter(now)) {
            return priceFeedCache;
        }

        synchronized (priceFeedLock) {
            now = Instant.now();
            if (!priceFeedCache.isEmpty() && priceFeedFetchedAt.plus(ttl).isAfter(now)) {
                return priceFeedCache;
            }
            if (priceFeedCache.isEmpty() && lastFailedFetchAt.plus(FAILED_FETCH_BACKOFF).isAfter(now)) {
                return priceFeedCache;
            }

            try {
                String json = restTemplate.getForObject(priceFeedUrl, String.class);
                Map<String, Object> body = json == null || json.isBlank()
                        ? Collections.emptyMap()
                        : objectMapper.readValue(json, new TypeReference<>() {});
                Map<String, Double> prices = parsePriceFeed(body);
                if (!prices.isEmpty()) {
                    priceFeedCache = Map.copyOf(prices);
                    priceFeedFetchedAt = Instant.now();
                    return priceFeedCache;
                }
                lastFailedFetchAt = Instant.now();
            } catch (Exception e) {
                lastFailedFetchAt = Instant.now();
                System.err.println("[PRICE] No se pudo cargar el feed de precios: " + e.getMessage());
            }
            return priceFeedCache;
        }
    }

    private Map<String, Double> parsePriceFeed(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return Collections.emptyMap();
        }

        Object dataNode = body.get("data");
        Object itemsNode = dataNode instanceof Map<?, ?> data
                ? data.get("items")
                : body.get("items");

        if (!(itemsNode instanceof List<?> items)) {
            return Collections.emptyMap();
        }

        Map<String, Double> prices = new ConcurrentHashMap<>();
        for (Object itemNode : items) {
            if (!(itemNode instanceof Map<?, ?> item)) continue;

            String marketHashName = asText(item.get("market_hash_name"));
            if (marketHashName.isBlank()) continue;

            Optional<Double> price = extractSteamPrice(item.get("prices"));
            price.ifPresent(value -> prices.put(normalizePriceKey(marketHashName), value));
        }
        return prices;
    }

    private Optional<Double> extractSteamPrice(Object pricesNode) {
        if (!(pricesNode instanceof List<?> prices)) {
            return Optional.empty();
        }

        Double firstPositivePrice = null;
        for (Object priceNode : prices) {
            if (!(priceNode instanceof Map<?, ?> priceData)) continue;

            Double price = asPositiveDouble(priceData.get("price"));
            if (price == null) continue;

            String provider = asText(priceData.get("provider"));
            if ("steam".equalsIgnoreCase(provider)) {
                return Optional.of(price);
            }
            if (firstPositivePrice == null) {
                firstPositivePrice = price;
            }
        }
        return Optional.ofNullable(firstPositivePrice);
    }

    private String resolveMarketHashName(Skin skin) {
        SkinCatalogo catalogo = skin.getCatalogo();
        List<String> candidates = new ArrayList<>();
        if (skin.getInventarioItem() != null) {
            candidates.add(skin.getInventarioItem().getMarketHashName());
        }
        if (catalogo != null) {
            candidates.add(catalogo.getMarketHashName());
            candidates.add(catalogo.getName());
        }
        candidates.add(skin.getName());

        for (String candidate : candidates) {
            String normalized = repairMarketHashName(normalizeMojibake(candidate), catalogo);
            if (!normalized.isBlank() && normalized.contains("(")) {
                return normalized;
            }
        }

        String baseName = candidates.stream()
                .map(this::normalizeMojibake)
                .map(value -> repairMarketHashName(value, catalogo))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
        String exterior = exteriorToSteamName(skin);
        if (!baseName.isBlank() && exterior != null && !baseName.contains("(")) {
            return baseName + " (" + exterior + ")";
        }
        return baseName;
    }

    private Set<String> resolveMarketHashNames(InventarioItem item) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (item == null) return candidates;

        addCandidate(candidates, item.getMarketHashName(), item.getCatalogo());
        addCandidate(candidates, item.getName(), item.getCatalogo());

        SkinCatalogo catalogo = item.getCatalogo();
        if (catalogo != null) {
            addCandidate(candidates, catalogo.getMarketHashName(), catalogo);
            addCandidate(candidates, catalogo.getName(), catalogo);
        }

        return candidates;
    }

    private void addCandidate(Set<String> candidates, String value, SkinCatalogo catalogo) {
        String normalized = normalizeMojibake(value);
        if (normalized.isBlank()) return;

        String repaired = repairMarketHashName(normalized, catalogo);
        if (!repaired.isBlank()) {
            candidates.add(repaired);
            if (repaired.contains("StatTrak\u2122")) {
                candidates.add(repaired.replace("StatTrak\u2122 ", ""));
            }
        }
    }

    private String repairMarketHashName(String value, SkinCatalogo catalogo) {
        String repaired = value
                .replace("StatTrakTM", "StatTrak\u2122")
                .replace("StatTrak\u2122\u00a0", "StatTrak\u2122 ")
                .replace("StatTrak ", "StatTrak\u2122 ")
                .replace("?\u2122", "\u2122")
                .replace("?\u00a2", "\u2122")
                .replace("\u0192??", "\u2605")
                .replace("?", "");

        repaired = repaired.replaceAll("\\s+", " ").trim();
        if (needsCollectibleStar(repaired, catalogo) && !repaired.startsWith("\u2605")) {
            repaired = "\u2605 " + repaired;
        }
        return repaired;
    }

    private boolean needsCollectibleStar(String marketHashName, SkinCatalogo catalogo) {
        String haystack = (marketHashName + " " +
                (catalogo != null ? catalogo.getWeaponName() + " " + catalogo.getCategoryName() : ""))
                .toLowerCase(Locale.ROOT);
        return haystack.contains("knife") ||
                haystack.contains("gloves") ||
                haystack.contains("bayonet") ||
                haystack.contains("karambit");
    }

    private Set<String> marketHashCandidates(String marketHashName) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalized = normalizeMojibake(marketHashName);
        addCandidate(candidates, normalized, null);
        addCandidate(candidates, tryDecodeMojibakeIfNeeded(normalized), null);
        return candidates;
    }

    private String normalizePriceKey(String value) {
        return normalizeMojibake(value).toLowerCase(Locale.ROOT);
    }

    private String normalizeMojibake(String value) {
        if (value == null) return "";
        String normalized = value.trim();
        for (int i = 0; i < 3; i++) {
            String decoded = tryDecodeMojibakeIfNeeded(normalized);
            if (decoded.equals(normalized)) break;
            normalized = decoded;
        }
        return normalized.replace("\u00C2", "").trim();
    }

    private String tryDecodeMojibakeIfNeeded(String value) {
        if (value == null || value.isBlank()) return "";
        if (!looksLikeMojibake(value)) return value;
        try {
            return new String(value.getBytes(WINDOWS_1252), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }

    private boolean looksLikeMojibake(String value) {
        return value.indexOf('\u00C3') >= 0 ||
                value.indexOf('\u00C2') >= 0 ||
                value.indexOf('\u00E2') >= 0 ||
                value.indexOf('\u00C5') >= 0 ||
                value.indexOf('\u00E5') >= 0 ||
                value.indexOf('\u00BC') >= 0;
    }

    private String exteriorToSteamName(Skin skin) {
        if (skin.getCatalogo() != null && skin.getCatalogo().getExteriorName() != null) {
            return skin.getCatalogo().getExteriorName();
        }
        if (skin.getExterior() == null) return null;
        return switch (skin.getExterior()) {
            case RECIEN_FABRICADO -> "Factory New";
            case CASI_NUEVO -> "Minimal Wear";
            case ALGO_DESGASTADO -> "Field-Tested";
            case BASTANTE_DESGASTADO -> "Well-Worn";
            case DEPLORABLE -> "Battle-Scarred";
        };
    }

    private double applyDiscount(double priceUsd) {
        double safeDiscountRate = Math.max(0.0, Math.min(discountRate, 1.0));
        return priceUsd * (1.0 - safeDiscountRate);
    }

    private Double asPositiveDouble(Object value) {
        if (value instanceof Number number) {
            double parsed = number.doubleValue();
            return parsed > 0 ? parsed : null;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                double parsed = Double.parseDouble(text);
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String asText(Object value) {
        return value instanceof String text ? text.trim() : "";
    }

    private double roundMoney(double value) {
        return BigDecimal.valueOf(Math.max(value, 0.01))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
