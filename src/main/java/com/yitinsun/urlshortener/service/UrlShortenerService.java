package com.yitinsun.urlshortener.service;

import com.yitinsun.urlshortener.config.AppProperties;
import com.yitinsun.urlshortener.domain.UrlMapping;
import com.yitinsun.urlshortener.encoding.Base62Encoder;
import com.yitinsun.urlshortener.repo.UrlMappingRepository;
import com.yitinsun.urlshortener.web.ResourceNotFoundException;
import com.yitinsun.urlshortener.web.dto.ShortenRequest;
import com.yitinsun.urlshortener.web.dto.ShortenResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

@Service
public class UrlShortenerService {

    static final int DEFAULT_TTL_DAYS = 730;
    static final int MIN_TTL_DAYS = 1;
    static final int MAX_TTL_DAYS = 3650;
    static final int MAX_URL_LENGTH = 2048;
    static final int MIN_CODE_LENGTH = 5;

    private final UrlMappingRepository repository;
    private final AppProperties appProperties;

    public UrlShortenerService(UrlMappingRepository repository, AppProperties appProperties) {
        this.repository = repository;
        this.appProperties = appProperties;
    }

    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        String longUrl = validateLongUrl(request.longUrl());
        int ttlDays = resolveTtl(request.ttlInDays());
        Instant now = Instant.now();

        return repository.findByLongUrlAndExpiresAtAfter(longUrl, now).stream()
                .filter(mapping -> longUrl.equals(mapping.getLongUrl()))
                .findFirst()
                .map(this::toResponse)
                .orElseGet(() -> createMapping(longUrl, ttlDays, now));
    }

    @Transactional(readOnly = true)
    public String resolveActiveLongUrl(String shortCode) {
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException(shortCode));
        if (mapping.getExpiresAt() == null || !mapping.getExpiresAt().isAfter(Instant.now())) {
            throw new ResourceNotFoundException(shortCode);
        }
        return mapping.getLongUrl();
    }

    private ShortenResponse createMapping(String longUrl, int ttlDays, Instant now) {
        UrlMapping mapping = new UrlMapping();
        mapping.setLongUrl(longUrl);
        mapping.setShortCode("tmp" + UUID.randomUUID().toString().replace("-", "").substring(0, 7));
        mapping.setCreatedAt(now);
        mapping.setExpiresAt(now.plus(ttlDays, ChronoUnit.DAYS));
        repository.saveAndFlush(mapping);
        mapping.setShortCode(padToMinLength(Base62Encoder.encode(mapping.getId())));
        repository.save(mapping);
        return toResponse(mapping);
    }

    private ShortenResponse toResponse(UrlMapping mapping) {
        String base = trimTrailingSlash(appProperties.baseUrl());
        return new ShortenResponse(
                mapping.getLongUrl(),
                base + "/short/" + mapping.getShortCode(),
                mapping.getShortCode(),
                mapping.getExpiresAt()
        );
    }

    String validateLongUrl(String longUrl) {
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("longUrl is required");
        }
        String trimmed = longUrl.trim();
        if (trimmed.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("longUrl exceeds 2048 characters");
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.contains("<script") || lower.startsWith("javascript:") || lower.startsWith("data:")) {
            throw new IllegalArgumentException("longUrl contains a disallowed payload");
        }
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("longUrl is not a valid URI");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("longUrl must be HTTP or HTTPS");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("longUrl must include a host");
        }
        rejectSelfLoop(uri);
        return trimmed;
    }

    private void rejectSelfLoop(URI longUri) {
        URI base = URI.create(appProperties.baseUrl());
        String destHost = longUri.getHost();
        String appHost = base.getHost();
        if (destHost != null && appHost != null && destHost.equalsIgnoreCase(appHost)) {
            throw new IllegalArgumentException("longUrl must not point at this service");
        }
    }

    int resolveTtl(Integer ttlInDays) {
        if (ttlInDays == null) {
            return DEFAULT_TTL_DAYS;
        }
        if (ttlInDays < MIN_TTL_DAYS || ttlInDays > MAX_TTL_DAYS) {
            throw new IllegalArgumentException("ttlInDays must be between 1 and 3650");
        }
        return ttlInDays;
    }

    static String padToMinLength(String code) {
        if (code.length() >= MIN_CODE_LENGTH) {
            return code;
        }
        return "0".repeat(MIN_CODE_LENGTH - code.length()) + code;
    }

    private static String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
