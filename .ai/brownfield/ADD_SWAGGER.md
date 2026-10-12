# Swagger UI (springdoc) — optional enhancement

Standalone playbook for adding **OpenAPI 3** docs and **Swagger UI** to this Spring Boot WebMVC app. Swagger does not change business behavior: it publishes a browsable description of HTTP endpoints that already exist and lets you Try-it-out.

This file is self-contained. It does not depend on other `.ai/**` documents. Copy the snippets as written, then adjust names if your classes differ.

**Library:** `springdoc-openapi-starter-webmvc-ui` (Jakarta / OpenAPI 3). Do not use Springfox or `io.swagger.annotations` (Swagger 2).

**Assumed local app (this repo)**

| Item | Value |
|:-----|:------|
| Spring Boot | 3.3.x (parent `3.3.5`) |
| Java | 17 |
| Root package | `com.yitinsun.urlshortener` |
| JSON API | `POST /api/v1/shorten` |
| Redirect (not JSON) | `GET /short/{shortCode}` → **302** + `Location` |
| DTO style | Java `record`s in `...web.dto` |
| Auth today | none — Security / JWT sections are optional add-ons |

Default after enablement:

- UI: `http://localhost:8080/swagger-ui.html`
- Spec: `http://localhost:8080/v3/api-docs` and `/v3/api-docs.yaml`

---

## 1. Maven (`pom.xml`)

Add a single dependency. Pin only the starter.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

2.6.x matches Spring Boot 3.3. One starter is enough: it serves both the JSON spec and the UI. Do not add `springdoc-openapi-starter-webflux-ui` alongside it.

Check: `mvn -q dependency:tree -Dincludes=org.springdoc`

---

## 2. YAML (`src/main/resources/application.yml`)

Append a shared `springdoc` block (any profile). Toggle with environment variables; do not put tokens or passwords here.

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: ${SPRINGDOC_ENABLED:true}
  swagger-ui:
    path: /swagger-ui.html
    enabled: ${SPRINGDOC_SWAGGER_UI_ENABLED:true}
    operations-sorter: method
    tags-sorter: alpha
    display-request-duration: true
    try-it-out-enabled: true
    persist-authorization: false
  show-actuator: false
  default-consumes-media-type: application/json
  default-produces-media-type: application/json
  packages-to-scan: com.yitinsun.urlshortener.web
  paths-to-match:
    - /api/v1/**
```

`paths-to-match: /api/v1/**` keeps the UI on the JSON API. `GET /short/{shortCode}` is a redirect hop; hide it by default (path filter and/or `@Hidden` in §4).

Optional: turn the UI off in production unless an env var turns it on.

```yaml
---
spring:
  config:
    activate:
      on-profile: prod
springdoc:
  api-docs:
    enabled: ${SPRINGDOC_ENABLED:false}
  swagger-ui:
    enabled: ${SPRINGDOC_SWAGGER_UI_ENABLED:false}
```

`/swagger-ui.html` redirects to `/swagger-ui/index.html`. Actuator is omitted (`show-actuator: false`); set `true` only if you want those endpoints in the same catalog.

---

## 3. General API metadata (`OpenAPI` bean)

YAML does not set title, description, or server URL. Add a configuration class so the UI header is explicit.

If the app already has `app.base-url` (this project: `AppProperties` record with `baseUrl()`), use it for the server list. Otherwise hardcode a server URL for local work.

```java
package com.yitinsun.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private final AppProperties appProperties;

    public OpenApiConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public OpenAPI urlShortenerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description(
                                "Create a short code from a long HTTP(S) URL. "
                                        + "Redirect (`GET /short/{shortCode}` → 302) is omitted from this UI.")
                        .version("0.1.0")
                        .contact(new Contact().name("yitinsun").url(appProperties.baseUrl()))
                        .license(new License().name("UNLICENSED")))
                .addServersItem(new Server()
                        .url(appProperties.baseUrl())
                        .description("This instance"));
    }
}
```

This bean is documentation only. It does not register filters or change controller mappings.

---

## 4. Annotating controllers and models

Use `io.swagger.v3.oas.annotations.*`. Annotations must not change paths, status codes, or validation.

### 4.1 REST controller (`POST /api/v1/shorten`)

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Shorten", description = "Create short links")
public class UrlShortenerController {

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
```

### 4.2 Redirect controller

Leave it out of the spec (path filter in §2). Extra belt-and-suspenders:

```java
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
public class RedirectController { /* existing mappings */ }
```

If you later want it listed, drop `@Hidden`, widen `paths-to-match`, and describe **302** (not 301) plus **404**.

### 4.3 Request / response records

Bean Validation already constrains fields; `@Schema` only feeds the UI.

```java
import io.swagger.v3.oas.annotations.media.Schema;

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
) {}
```

Annotate `ShortenResponse` (`longUrl`, `shortUrl`, `shortCode`, `expiresAt`) and `ErrorResponse` (`status`, `error`, `message`, `shortCode`) the same way. `@ControllerAdvice` needs no OpenAPI types unless you want `@ExampleObject` payloads.

---

## 5. Spring Security permit rules

`ENHANCE.md` added `spring-boot-starter-security` as a **permitAll** demo (`SecurityConfig`). Swagger still needs these matchers. If they are missing, the UI returns 401/403:

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/v3/api-docs",
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            );
    return http.build();
}
```

Keep public business routes (for example `POST /api/v1/shorten`) in `permitAll()` if they are meant to stay unauthenticated. Order matchers from most specific to `anyRequest()`.

---

## 6. JWT / Bearer in Swagger UI (only if the API uses Bearer tokens)

Skip this while the API is open. When operations require `Authorization: Bearer <access-token>`, declare a scheme so the UI shows **Authorize**.

On the `OpenAPI` bean:

```java
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

new OpenAPI()
        // .info(...) .servers(...) as in §3
        .components(new Components().addSecuritySchemes(
                "bearer-jwt",
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
```

Global `addSecurityItem` applies to every operation. To lock only some methods, omit the global requirement and put `@SecurityRequirement(name = "bearer-jwt")` on those methods.

Optional YAML so the browser remembers the last pasted token for the session:

```yaml
springdoc:
  swagger-ui:
    persist-authorization: true
```

In the UI: **Authorize** → paste the raw token (no `Bearer ` prefix). Do not commit tokens, signing keys, or IdP secrets into YAML or this file.

---

## 7. Smoke check

1. Start the app (H2 test profile or MySQL `dev`).
2. Open `http://localhost:8080/swagger-ui.html`.
3. Header shows **URL Shortener API** `0.1.0`.
4. Catalog lists `POST /api/v1/shorten` (and not `GET /short/{shortCode}` unless you opted in).
5. Try-it-out with a valid `https://…` URL returns **201** and a `shortCode`.
6. `GET http://localhost:8080/v3/api-docs` is OpenAPI 3 JSON.

If the UI is blank: confirm the starter is on the **runtime** classpath (not `test` scope) and `springdoc.swagger-ui.enabled` is true for that profile.

If Try-it-out gets 401: apply §5; if the API needs a token, apply §6.

---

## 8. What this enhancement is not

- Not a new product API, schema, or auth system
- Not a replacement for integration tests
- Not Springfox, Gateway, or WebFlux docs
- Not something that should wrap JPA entities as public schemas

---

## 9. Touch list

| Location | Change |
|:---------|:-------|
| `pom.xml` | springdoc WebMVC UI starter 2.6.0 |
| `application.yml` | `springdoc` block (optional `prod` disable) |
| `...config.OpenApiConfig` | `OpenAPI` `@Bean` |
| `UrlShortenerController` | `@Tag` / `@Operation` / `@ApiResponses` |
| DTO `record`s | `@Schema` |
| `RedirectController` | `@Hidden` (recommended) |
| Security / JWT classes | only if those features already exist |

---

## 10. File name

This playbook is `.ai/brownfield/ADD_SWAGGER.md`: an action name for adding OpenAPI + Swagger UI. `AAA_` was only a sort prefix and is not used.
