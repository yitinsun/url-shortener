package com.yitinsun.urlshortener.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegalPathsTest {

    @Test
    void allowsDocumentedGetAndPostRoutes() {
        assertThat(LegalPaths.isAllowed("GET", "/actuator")).isTrue();
        assertThat(LegalPaths.isAllowed("GET", "/actuator/health")).isTrue();
        assertThat(LegalPaths.isAllowed("POST", "/api/v1/shorten")).isTrue();
        assertThat(LegalPaths.isAllowed("GET", "/short/b9K2x1")).isTrue();
        assertThat(LegalPaths.isAllowed("GET", "/swagger-ui.html")).isTrue();
        assertThat(LegalPaths.isAllowed("GET", "/swagger-ui/index.html")).isTrue();
        assertThat(LegalPaths.isAllowed("GET", "/v3/api-docs")).isTrue();
        assertThat(LegalPaths.isAllowed("GET", "/v3/api-docs.yaml")).isTrue();
        assertThat(LegalPaths.isAllowed("GET", "/error")).isTrue();
    }

    @Test
    void rejectsUndocumentedRoutesAndWrongMethods() {
        assertThat(LegalPaths.isAllowed("GET", "/")).isFalse();
        assertThat(LegalPaths.isAllowed("GET", "/nope")).isFalse();
        assertThat(LegalPaths.isAllowed("GET", "/b9K2x1")).isFalse();
        assertThat(LegalPaths.isAllowed("GET", "/api/v1/shorten")).isFalse();
        assertThat(LegalPaths.isAllowed("GET", "/short/")).isFalse();
        assertThat(LegalPaths.isAllowed("POST", "/short/b9K2x1")).isFalse();
        assertThat(LegalPaths.isAllowed("PUT", "/api/v1/shorten")).isFalse();
    }
}
