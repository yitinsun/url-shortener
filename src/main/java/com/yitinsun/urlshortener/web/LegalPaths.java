package com.yitinsun.urlshortener.web;

import java.util.Locale;
import java.util.regex.Pattern;

public final class LegalPaths {

    private static final Pattern SHORT_CODE =
            Pattern.compile("^/short/[0-9A-Za-z]{1,10}$");

    private LegalPaths() {
    }

    public static boolean isAllowed(String method, String requestUri) {
        String path = normalize(requestUri);
        String verb = method == null ? "" : method.toUpperCase(Locale.ROOT);
        if ("/error".equals(path)) {
            return true;
        }
        boolean read = "GET".equals(verb) || "HEAD".equals(verb);
        if (read) {
            if ("/actuator".equals(path) || path.startsWith("/actuator/")) {
                return true;
            }
            if ("/swagger-ui.html".equals(path) || path.startsWith("/swagger-ui/")) {
                return true;
            }
            if ("/v3/api-docs".equals(path)
                    || "/v3/api-docs.yaml".equals(path)
                    || path.startsWith("/v3/api-docs/")) {
                return true;
            }
            if (SHORT_CODE.matcher(path).matches()) {
                return true;
            }
        }
        return "POST".equals(verb) && "/api/v1/shorten".equals(path);
    }

    static String normalize(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "/";
        }
        String path = requestUri;
        int session = path.indexOf(';');
        if (session >= 0) {
            path = path.substring(0, session);
        }
        if (path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
