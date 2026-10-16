package com.yitinsun.urlshortener.service;

import com.yitinsun.urlshortener.config.AppProperties;
import com.yitinsun.urlshortener.domain.UrlMapping;
import com.yitinsun.urlshortener.encoding.Base62Encoder;
import com.yitinsun.urlshortener.repo.UrlMappingRepository;
import com.yitinsun.urlshortener.web.ResourceNotFoundException;
import com.yitinsun.urlshortener.web.dto.ShortenRequest;
import com.yitinsun.urlshortener.web.dto.ShortenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(repository, new AppProperties("http://localhost:8080"));
    }

    @Test
    void missingResourceThrowsResourceNotFoundException() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveActiveLongUrl("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found or expired");
    }

    @Test
    void expiredMappingThrowsResourceNotFoundException() {
        UrlMapping expired = new UrlMapping();
        expired.setShortCode("abcde");
        expired.setLongUrl("https://example.com");
        expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(repository.findByShortCode("abcde")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolveActiveLongUrl("abcde"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void idempotentCreateReturnsExistingActiveRowWithoutSecondInsert() {
        UrlMapping existing = new UrlMapping();
        existing.setLongUrl("https://example.com/path");
        existing.setShortCode("exist1");
        existing.setExpiresAt(Instant.now().plus(10, ChronoUnit.DAYS));
        when(repository.findByLongUrlAndExpiresAtAfter(eq("https://example.com/path"), any(Instant.class)))
                .thenReturn(List.of(existing));

        ShortenResponse response = service.shorten(new ShortenRequest("https://example.com/path", 30));

        assertThat(response.shortCode()).isEqualTo("exist1");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/short/exist1");
        verify(repository, never()).save(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void defaultTtlIs730DaysWhenOmitted() {
        when(repository.findByLongUrlAndExpiresAtAfter(eq("https://example.com/new"), any(Instant.class)))
                .thenReturn(List.of());
        when(repository.saveAndFlush(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping mapping = invocation.getArgument(0);
            mapping.setId(56_800_235L);
            return mapping;
        });
        when(repository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        ShortenResponse response = service.shorten(new ShortenRequest("https://example.com/new", null));
        Instant expected = before.plus(730, ChronoUnit.DAYS);

        assertThat(response.expiresAt()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
        assertThat(response.shortCode()).isEqualTo(Base62Encoder.encode(56_800_235L));

        ArgumentCaptor<UrlMapping> captor = ArgumentCaptor.forClass(UrlMapping.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
    }

    @Test
    void selfLoopUrlIsRejected() {
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("http://localhost:8080/already-short", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not point at this service");
    }

    @Test
    void oversizeUrlIsRejected() {
        String oversize = "https://example.com/" + "a".repeat(2048);
        assertThat(oversize.length()).isGreaterThan(2048);
        assertThatThrownBy(() -> service.shorten(new ShortenRequest(oversize, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2048");
    }

    @Test
    void scriptAndNonHttpSchemesAreRejected() {
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("javascript:alert(1)", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com/<script>x</script>", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("ftp://example.com/file", 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://", 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidTtlIsRejected() {
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com/x", 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.shorten(new ShortenRequest("https://example.com/x", 3651)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolveReturnsLongUrlWhenActive() {
        UrlMapping active = new UrlMapping();
        active.setShortCode("ok123");
        active.setLongUrl("https://example.com/ok");
        active.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(repository.findByShortCode("ok123")).thenReturn(Optional.of(active));

        assertThat(service.resolveActiveLongUrl("ok123")).isEqualTo("https://example.com/ok");
    }
}
