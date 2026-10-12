package com.yitinsun.urlshortener.web;

import com.yitinsun.urlshortener.service.UrlShortenerService;
import com.yitinsun.urlshortener.web.dto.ErrorResponse;
import com.yitinsun.urlshortener.web.dto.ShortenRequest;
import com.yitinsun.urlshortener.web.dto.ShortenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Shorten", description = "Create short links")
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    public UrlShortenerController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @Operation(summary = "Create a short URL")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Mapping created, or an existing active mapping returned",
                    content = @Content(schema = @Schema(implementation = ShortenResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid body (URL, TTL, or self-loop)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "503",
                    description = "Database unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlShortenerService.shorten(request));
    }
}
