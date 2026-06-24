package skinsmarket.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SteamMarketPriceService {

    private static final String PRICE_OVERVIEW_URL =
            "https://steamcommunity.com/market/priceoverview/";
    private static final int CS2_APP_ID = 730;
    private static final int ARS_CURRENCY_ID = 34;
    private static final Duration CACHE_TTL = Duration.ofMinutes(20);
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    @Autowired
    private RestTemplate restTemplate;

    @Value("${application.balance.usd-to-ars:1451.02}")
    private double usdToArs;

    @Value("${steam.market.discount-rate:0.08}")
    private double discountRate;

    @Value("${steam.market.request-spacing-ms:350}")
    private long requestSpacingMs;

    private final Map<String, CachedPrice> cache = new ConcurrentHashMap<>();
    private final Object steamRequestLock = new Object();
    private Instant nextSteamRequestAt = Instant.EPOCH;
    private Instant rateLimitedUntil = Instant.EPOCH;

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
        for (String marketHashName : marketHashNames) {
            Optional<Double> price = findSinglePriceUsd(marketHashName);
            if (price.isPresent()) return price;
        }
        return Optional.empty();
    }

    private Optional<Double> findSinglePriceUsd(String marketHashName) {
        if (marketHashName == null || marketHashName.isBlank()) {
            return Optional.empty();
        }

        String key = normalizeMojibake(marketHashName).toLowerCase();
        CachedPrice cached = cache.get(key);
        if (cached != null && cached.isFresh()) {
            return Optional.of(cached.priceUsd());
        }

        Optional<Double> steamPriceArs = fetchSteamPriceArs(marketHashName);
        if (steamPriceArs.isEmpty()) {
            return Optional.empty();
        }

        double priceUsd = roundMoney(toDiscountedUsd(steamPriceArs.get()));
        cache.put(key, new CachedPrice(priceUsd, Instant.now()));
        return Optional.of(priceUsd);
    }

    private Optional<Double> fetchSteamPriceArs(String marketHashName) {
        if (Instant.now().isBefore(rateLimitedUntil)) {
            return Optional.empty();
        }

        for (String candidate : marketHashCandidates(marketHashName)) {
            try {
                waitForSteamSlot();

                String url = UriComponentsBuilder
                        .fromUriString(PRICE_OVERVIEW_URL)
                        .queryParam("appid", CS2_APP_ID)
                        .queryParam("currency", ARS_CURRENCY_ID)
                        .queryParam("market_hash_name", candidate)
                        .encode()
                        .toUriString();

                @SuppressWarnings("unchecked")
                Map<String, Object> body = restTemplate.getForObject(url, Map.class);
                if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
                    continue;
                }

                String priceText = firstText(body.get("median_price"), body.get("lowest_price"));
                if (priceText == null) continue;

                Optional<Double> price = parseSteamMoney(priceText);
                if (price.isPresent()) return price;
            } catch (HttpClientErrorException.TooManyRequests e) {
                rateLimitedUntil = Instant.now().plus(Duration.ofSeconds(60));
                return Optional.empty();
            } catch (Exception ignored) {
                // Steam can rate-limit a single candidate. Try the next normalized form.
            }
        }
        return Optional.empty();
    }

    private void waitForSteamSlot() {
        synchronized (steamRequestLock) {
            Instant now = Instant.now();
            if (now.isBefore(nextSteamRequestAt)) {
                try {
                    Thread.sleep(Duration.between(now, nextSteamRequestAt).toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            nextSteamRequestAt = Instant.now().plusMillis(Math.max(requestSpacingMs, 0));
        }
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
                .toLowerCase();
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

    private double toDiscountedUsd(double steamPriceArs) {
        double effectiveRate = usdToArs > 0 ? usdToArs : 1451.02;
        double safeDiscountRate = Math.max(0.0, Math.min(discountRate, 1.0));
        double discountedArs = steamPriceArs * (1.0 - safeDiscountRate);
        return discountedArs / effectiveRate;
    }

    private Optional<Double> parseSteamMoney(String raw) {
        String normalized = raw.replaceAll("[^0-9,.]", "");
        if (normalized.isBlank()) return Optional.empty();

        int comma = normalized.lastIndexOf(',');
        int dot = normalized.lastIndexOf('.');
        if (comma >= 0 && comma > dot) {
            normalized = normalized.replace(".", "").replace(',', '.');
        } else {
            normalized = normalized.replace(",", "");
        }

        try {
            return Optional.of(Double.parseDouble(normalized));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private String firstText(Object first, Object second) {
        if (first instanceof String value && !value.isBlank()) return value;
        if (second instanceof String value && !value.isBlank()) return value;
        return null;
    }

    private double roundMoney(double value) {
        return BigDecimal.valueOf(Math.max(value, 0.01))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record CachedPrice(double priceUsd, Instant createdAt) {
        boolean isFresh() {
            return createdAt.plus(CACHE_TTL).isAfter(Instant.now());
        }
    }
}
