package com.yitinsun.urlshortener.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ShortenResponse", description = "Created or reused short-link payload")
public record ShortenResponse(
        @Schema(description = "Original destination URL", example = "https://example.com/very/long/path")
        String longUrl,
        @Schema(description = "Shareable short URL", example = "http://localhost:8080/short/b9K2x1")
        String shortUrl,
        @Schema(description = "Base62 short code", example = "b9K2x1")
        String shortCode,
        @Schema(description = "Expiration instant in UTC", example = "2027-10-09T09:25:00Z")
        Instant expiresAt
) {
}
