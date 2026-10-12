package com.yitinsun.urlshortener.web;

import com.yitinsun.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    public RedirectController(UrlShortenerService urlShortenerService) {
        this.urlShortenerService = urlShortenerService;
    }

    @GetMapping("/short/{shortCode:[0-9A-Za-z]{1,10}}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = urlShortenerService.resolveActiveLongUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, longUrl)
                .build();
    }
}
