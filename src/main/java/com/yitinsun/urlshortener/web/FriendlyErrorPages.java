package com.yitinsun.urlshortener.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class FriendlyErrorPages {

    private FriendlyErrorPages() {
    }

    public static ResponseEntity<String> entity(int status, String reason, String requestUri) {
        HttpStatus httpStatus = HttpStatus.resolve(status);
        if (httpStatus == null) {
            httpStatus = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(httpStatus)
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html(reason, requestUri));
    }

    public static void write(HttpServletResponse response, int status, String reason, String requestUri)
            throws IOException {
        byte[] body = html(reason, requestUri).getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    static String html(String reason, String requestUri) {
        String path = escape(requestUri == null || requestUri.isBlank() ? "/" : requestUri);
        String detail = escape(reason == null ? "This URL is not available." : reason);
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <title>Page not found</title>
                  <style>
                    body { font-family: system-ui, sans-serif; max-width: 40rem; margin: 3rem auto; color: #222; }
                    a { color: #0b57d0; }
                    code { background: #f4f4f4; padding: 0.1rem 0.35rem; }
                  </style>
                </head>
                <body>
                  <h1>We could not find that page</h1>
                  <p>The URL shortener does not serve <code>%s</code>.</p>
                  <p>%s</p>
                  <p>You can:</p>
                  <ul>
                    <li>Open the API docs at <a href="/swagger-ui.html">/swagger-ui.html</a></li>
                    <li>Create a link with <code>POST /api/v1/shorten</code></li>
                    <li>Follow a short link at <code>/short/{code}</code></li>
                    <li>Check process health at <a href="/actuator">/actuator</a></li>
                  </ul>
                </body>
                </html>
                """.formatted(path, detail);
    }

    static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
