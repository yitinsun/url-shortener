package com.yitinsun.urlshortener.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LegalPathFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = pathWithinApp(request);
        if (LegalPaths.isAllowed(request.getMethod(), path)) {
            filterChain.doFilter(request, response);
            return;
        }
        FriendlyErrorPages.write(response, 404, "Unknown or disallowed URL", path);
    }

    static String pathWithinApp(HttpServletRequest request) {
        String uri = request.getRequestURI() == null ? "/" : request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        return LegalPaths.normalize(uri);
    }
}
