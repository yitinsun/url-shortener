package com.yitinsun.urlshortener.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class HtmlSanitizeFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final HandlerExceptionResolver exceptionResolver;

    public HtmlSanitizeFilter(
            ObjectMapper objectMapper,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.objectMapper = objectMapper;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest effective = request;
        if (shouldInspect(request)) {
            RepeatableBodyRequestWrapper wrapped = new RepeatableBodyRequestWrapper(request);
            effective = wrapped;
            try {
                inspectJsonBody(wrapped);
            } catch (SanitizationException ex) {
                exceptionResolver.resolveException(wrapped, response, null, ex);
                return;
            }
        }
        filterChain.doFilter(effective, response);
    }

    private static boolean shouldInspect(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = LegalPathFilter.pathWithinApp(request);
        if (!"/api/v1/shorten".equals(path)) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private void inspectJsonBody(RepeatableBodyRequestWrapper request) throws IOException {
        String body = request.bodyAsString();
        if (body.isBlank()) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (JsonProcessingException ignored) {
            return;
        }
        JsonNode longUrl = root.get("longUrl");
        if (longUrl != null && longUrl.isTextual() && HtmlSanitizer.looksUnsafe(longUrl.asText())) {
            throw new SanitizationException("longUrl contains disallowed HTML");
        }
    }
}
