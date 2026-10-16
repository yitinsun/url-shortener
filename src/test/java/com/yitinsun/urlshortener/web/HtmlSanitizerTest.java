package com.yitinsun.urlshortener.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    @Test
    void plainHttpUrlIsSafe() {
        assertThat(HtmlSanitizer.looksUnsafe("https://example.com/system-design")).isFalse();
        assertThat(HtmlSanitizer.looksUnsafe("")).isFalse();
        assertThat(HtmlSanitizer.looksUnsafe(null)).isFalse();
    }

    @Test
    void htmlAndScriptMarkupIsUnsafe() {
        assertThat(HtmlSanitizer.looksUnsafe("https://example.com/<script>alert(1)</script>")).isTrue();
        assertThat(HtmlSanitizer.looksUnsafe("<img src=x onerror=alert(1)>")).isTrue();
        assertThat(HtmlSanitizer.looksUnsafe("<b>bold</b>")).isTrue();
    }
}
