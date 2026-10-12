package com.yitinsun.urlshortener.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse", description = "Structured error body")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "404")
        int status,
        @Schema(description = "HTTP reason phrase", example = "Not Found")
        String error,
        @Schema(description = "Human-readable detail", example = "Short code not found or expired")
        String message,
        @Schema(description = "Short code when the error is about a lookup", example = "b9K2x1")
        String shortCode
) {
}
