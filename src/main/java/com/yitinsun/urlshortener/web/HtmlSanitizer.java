package com.yitinsun.urlshortener.web;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    public static boolean looksUnsafe(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (!Jsoup.isValid(value, Safelist.none())) {
            return true;
        }
        return Jsoup.parseBodyFragment(value).body().childrenSize() > 0;
    }
}
