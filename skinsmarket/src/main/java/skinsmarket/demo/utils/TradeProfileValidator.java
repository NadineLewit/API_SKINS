package skinsmarket.demo.utils;

import skinsmarket.demo.entity.User;

import java.net.URI;

public final class TradeProfileValidator {

    private TradeProfileValidator() {}

    public static void requireTradeUrl(User user, String action) {
        if (user == null || !isValidTradeUrl(user.getTradeUrl())) {
            throw new RuntimeException(
                    "Necesitás configurar una Steam Trade URL válida en Mi cuenta antes de " +
                            action + ". Pegá el link completo con partner y token.");
        }
    }

    public static void requireAliasCobro(User user, String action) {
        if (user == null || user.getAliasCobro() == null || user.getAliasCobro().isBlank()) {
            throw new RuntimeException(
                    "Necesitás configurar tu alias de cobro en Mi cuenta antes de " +
                            action + ".");
        }
    }

    public static boolean isValidTradeUrl(String tradeUrl) {
        if (tradeUrl == null || tradeUrl.isBlank()) return false;

        try {
            URI uri = URI.create(tradeUrl.trim());
            String host = uri.getHost();
            String path = uri.getPath();
            String query = uri.getRawQuery();

            boolean validHost = "steamcommunity.com".equalsIgnoreCase(host)
                    || "www.steamcommunity.com".equalsIgnoreCase(host);
            boolean validPath = path != null && path.startsWith("/tradeoffer/new");

            return validHost
                    && validPath
                    && hasQueryParam(query, "partner")
                    && hasQueryParam(query, "token");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean hasQueryParam(String query, String name) {
        if (query == null || query.isBlank()) return false;

        String prefix = name + "=";
        for (String part : query.split("&")) {
            if (part.startsWith(prefix) && part.length() > prefix.length()) {
                return true;
            }
        }
        return false;
    }
}
