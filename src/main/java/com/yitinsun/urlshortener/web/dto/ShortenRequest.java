package com.yitinsun.urlshortener.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ShortenRequest", description = "Create-short-link body")
public record ShortenRequest(
        @Schema(
                description = "Destination HTTP or HTTPS URL",
                example = "https://example.com/very/long/path",
                maxLength = 2048,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 2048) String longUrl,
        @Schema(
                description = "Lifetime in days; omit for the service default (730). Allowed 1–3650.",
                example = "365",
                minimum = "1",
                maximum = "3650")
        @Min(1) @Max(3650) Integer ttlInDays
) {
}
