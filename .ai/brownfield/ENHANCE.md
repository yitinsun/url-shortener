# Demo: OncePerRequestFilter, Spring Security, and Jsoup

Standalone brownfield note. This is a **demo** of how those pieces fit this Spring Boot 3.3 WebMVC app. It is not a product auth system (no users, JWT, or login page).

What it adds:

1. **Spring Security** `SecurityFilterChain` — CSRF off for the JSON API; documented routes `permitAll()`.
2. A second **`OncePerRequestFilter`** (`HtmlSanitizeFilter`) — Jsoup checks `longUrl` (and similar text) for HTML/script markup.
3. On a finding, the filter **does not invent a new error format**. It throws `SanitizationException` and lets **`GlobalExceptionHandler`** (via `HandlerExceptionResolver`) return the existing **400 JSON** for `POST /api/v1/shorten`.

`LegalPathFilter` (already an `OncePerRequestFilter`) stays the allowlist fence. This demo filter runs **after** that fence so illegal URLs still get the friendly HTML 404.

---

## 1. Maven (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.18.1</version>
</dependency>
```

Do not pin Spring Security; Boot 3.3.5’s parent BOM does that. Do not add `spring-security-oauth2` or JWT libraries for this demo.

---

## 2. Spring Security (demo config)

Create `config/SecurityConfig.java`. Goals:

- Do **not** require a login for shorten / redirect / Swagger / actuator.
- Disable CSRF so `POST /api/v1/shorten` from Swagger and curl keeps working.
- Disable `formLogin` / `httpBasic` so `/login` is not a new surface.
- `anyRequest().permitAll()` so unknown paths still reach `LegalPathFilter` HTML 404 (not a 401/403 login).

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator", "/actuator/**",
                                "/api/v1/shorten",
                                "/short/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
```

To lock the API later, change `/api/v1/shorten` (or `anyRequest()`) to `authenticated()` and add a real user store. That is **not** this demo.

---

## 3. OncePerRequestFilter + Jsoup

### 3.1 Repeatable body

The filter must read JSON **and** leave the body for `UrlShortenerController`. Wrap POST JSON in a request that buffers bytes and replays `getInputStream()` / `getReader()`.

### 3.2 Jsoup check

Treat markup as unsafe. Plain `https://example.com/path` must pass.

```java
static boolean looksUnsafe(String value) {
    if (value == null || value.isBlank()) {
        return false;
    }
    if (!Jsoup.isValid(value, Safelist.none())) {
        return true;
    }
    return Jsoup.parseBodyFragment(value).body().childrenSize() > 0;
}
```

Apply to `longUrl` on `POST /api/v1/shorten`. Do not mutate the URL in place for this demo (reject rather than silently `Jsoup.clean`).

### 3.3 Filter sketch

Order **after** `LegalPathFilter` (`HIGHEST_PRECEDENCE + 30`).

On unsafe input: `SanitizationException` → `HandlerExceptionResolver.resolveException(...)` → stop the chain.

That reuses `GlobalExceptionHandler` (400 JSON with `status` / `error` / `message`).

---

## 4. Error handling

| Where | Result |
|:------|:-------|
| Jsoup finding on `POST /api/v1/shorten` | **400** `ErrorResponse` JSON |
| Illegal path | Unchanged: `LegalPathFilter` **404 HTML** |
| Missing short code | Unchanged: **404 HTML** |

Do not show Whitelabel. Do not add a second error JSON shape.

---

## 5. Tests

- Existing ITs still pass (Security must not CSRF-block POST).
- POST `longUrl` containing HTML/script → **400** JSON, message mentions sanitization / HTML.
- `HtmlSanitizer` unit tests: clean URL vs `<script>` / `<img>`.

---

## 6. File list

| File | Role |
|:-----|:-----|
| `pom.xml` | `spring-boot-starter-security`, `jsoup` |
| `config/SecurityConfig.java` | Demo `SecurityFilterChain` |
| `web/RepeatableBodyRequestWrapper.java` | Replay POST body |
| `web/HtmlSanitizer.java` | Jsoup `looksUnsafe` |
| `web/SanitizationException.java` | Signals handler |
| `web/HtmlSanitizeFilter.java` | `OncePerRequestFilter` demo |
| `web/GlobalExceptionHandler.java` | Map `SanitizationException` → 400 |
| Tests | Sanitizer + IT for HTML `longUrl` |

---

## 7. File name

This playbook is `.ai/brownfield/ENHANCE.md`.
