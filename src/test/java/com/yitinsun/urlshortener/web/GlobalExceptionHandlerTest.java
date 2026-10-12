package com.yitinsun.urlshortener.web;

import com.yitinsun.urlshortener.web.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void dataAccessMapsTo503() {
        ResponseEntity<ErrorResponse> response =
                handler.unavailable(new QueryTimeoutException("lock"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
    }
}
