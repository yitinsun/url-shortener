package com.yitinsun.urlshortener.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FriendlyErrorPagesTest {

    @Test
    void htmlExplainsTheMissingPathAndEscapesMarkup() {
        String page = FriendlyErrorPages.html("Unknown or disallowed URL", "/<script>x");
        assertThat(page).contains("We could not find that page");
        assertThat(page).contains("/swagger-ui.html");
        assertThat(page).contains("POST /api/v1/shorten");
        assertThat(page).doesNotContain("<script>x");
        assertThat(page).contains("&lt;script&gt;x");
        assertThat(page).doesNotContain("Whitelabel");
    }
}
