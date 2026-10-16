package com.yitinsun.urlshortener.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class FriendlyErrorControllerTest {

    private final FriendlyErrorController controller = new FriendlyErrorController();

    @Test
    void rendersHtmlForDispatcher404() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/nope");

        ResponseEntity<String> response = controller.error(request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("We could not find that page");
        assertThat(response.getBody()).contains("/nope");
        assertThat(response.getBody()).doesNotContain("Whitelabel");
    }

    @Test
    void parsesStringStatusCodes() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, "500");

        ResponseEntity<String> response = controller.error(request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).contains("could not complete this request");
    }
}
