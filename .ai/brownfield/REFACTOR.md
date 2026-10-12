# Brownfield refactor — paths, allowlist, friendly errors

Standalone playbook. It does not depend on other `.ai/**` files, but **every related file listed here must be updated in the same change**. Do not leave old paths or `.ai/creation` references behind.

This is a refactor of the running Spring Boot 3.3 URL shortener (`com.yitinsun.urlshortener`), not a new product.

---

## Goals

1. Rename folder **`.ai/creation` → `.ai/greenfield`**. Update every pointer to that path.
2. Change redirect API **`GET /{shortCode}` → `GET /short/{shortCode}`**. Build `shortUrl` as `{APP_BASE_URL}/short/{shortCode}`.
3. Treat only the **allowlist** below as legal URLs. Everything else is illegal.
4. Illegal or not-found URLs return a **friendly HTML explanation page**. Do not show Spring **Whitelabel Error Page**. Do not leave the browser looking like a dead host (“This localhost page can’t be found”) **when this app is running**.

Chrome’s “This localhost page can’t be found” / `ERR_CONNECTION_REFUSED` happens when **nothing is listening on 8080**. That is not an application 404. After this refactor, any request that **does** reach Tomcat on an illegal or missing path must get our HTML page with HTTP **404**.

---

## 1. Rename `.ai/creation` → `.ai/greenfield`

Move the directory (keep the same markdown files inside). Then replace the string `.ai/creation` with `.ai/greenfield` everywhere it appears.

**Must update**

| Location | What to change |
|:---------|:---------------|
| `.ai/creation/` (folder) | Rename to `.ai/greenfield/` (`git mv` if tracked) |
| `AGENTS.md` | All `.ai/creation/` paths |
| `.cursor/rules/lifecycle-governance.mdc` | `INSTRUCTIONS.md` / `ORCHESTRATION_MANIFEST.md` paths |
| `.cursor/rules/url-shortener-implementation.mdc` | Spec paths |
| `.ai/greenfield/INSTRUCTIONS.md` | “this `.ai/creation` folder” |
| `.ai/greenfield/ORCHESTRATION_MANIFEST.md` | “Do not reset `.ai/creation`” |
| Any other grep hit for `.ai/creation` | Same replacement |

Do not leave an empty `.ai/creation` folder. Do not duplicate files in both names.

---

## 2. Redirect path `GET /short/{shortCode}`

Today `RedirectController` is `@GetMapping("/{shortCode}")` and `UrlShortenerService.toResponse` builds `base + "/" + shortCode` (example `http://localhost:8080/b9K2x1`). Both must change.

**Behavior (unchanged except the path prefix)**

| Condition | Response |
|:----------|:---------|
| Mapping exists and not expired | **302 Found**, `Location: <longUrl>` (never 301) |
| Missing or expired code | **404** friendly HTML page (§4), not JSON, not Whitelabel |

**Must update**

| Location | Change |
|:---------|:-------|
| `RedirectController.java` | `@GetMapping("/short/{shortCode}")`. Keep `@Hidden` if Swagger should omit redirect. |
| `UrlShortenerService.java` | `shortUrl` = `base + "/short/" + shortCode` |
| `UrlShortenerServiceTest.java` | Expected `shortUrl` (today `http://localhost:8080/exist1` → `http://localhost:8080/short/exist1`) |
| `UrlShortenerControllerIT.java` | `get("/" + code)` → `get("/short/" + code)`; `get("/zzzzz")` → `get("/short/zzzzz")`; `shortUrl` prefix `http://localhost:8080/short/`; OpenAPI path `/{shortCode}` check → `/short/{shortCode}` still **absent** from the spec |
| `OpenApiConfig.java` | Description text: `GET /short/{shortCode}` |
| `ShortenResponse` `@Schema` | Example `http://localhost:8080/short/b9K2x1` |
| `.ai/brownfield/ADD_SWAGGER.md` | Table and samples that still say `GET /{shortCode}` |
| `.ai/greenfield/PRD.md` | Endpoint and `shortUrl` formula |
| `.ai/greenfield/SYSTEM_DESIGN.md` | Flow diagram `GET /{shortCode}` |
| `.ai/greenfield/TESTING_PLAN.md` | Section `GET /{shortCode}` |
| `.ai/greenfield/URL_SHORTENER_SUMMARY.md` | Only if it mentions the old path |

Self-loop checks that compare against `APP_BASE_URL` stay valid; they reject destinations that point at this service, independent of `/short/`.

---

## 3. Legal URL allowlist

Only these request patterns are **legal**. Host is typically `http://localhost:8080` in local `dev`/`test`.

| Legal pattern | Methods | Notes |
|:--------------|:--------|:------|
| `/actuator` | GET | Actuator index (`spring-boot-starter-actuator` is already on the classpath) |
| `/actuator/**` | GET | Health and other exposed actuator resources |
| `/api/v1/shorten` | **POST** | JSON create API. GET/PUT/DELETE on this path are illegal |
| `/short/{shortCode}` | **GET** | Redirect. `{shortCode}` is Base62 `[0-9A-Za-z]{1,10}` |
| `/swagger-ui.html` | GET | Redirects to the UI |
| `/swagger-ui/**` | GET | Required so `index.html`, CSS, and JS load (implied by Swagger UI) |
| `/v3/api-docs` | GET | OpenAPI JSON |
| `/v3/api-docs.yaml` | GET | OpenAPI YAML |
| `/v3/api-docs/**` | GET | Springdoc extras |

**Illegal (examples):** `/`, `/index.html`, `/foo`, `/b9K2x1` (old redirect path), `/api/v1/shorten` with GET, `/short/` with no code, `/swagger-ui` without the documented suffix if unmatched, any other path.

Implement the allowlist as a **servlet filter** or **`HandlerInterceptor`** registered on `/**`:

1. If the request matches a legal pattern → `chain.doFilter` / `true`.
2. Else → **404** + friendly HTML (§4). Do not forward to a controller that might 302 or return JSON.

Keep Spring MVC mappings for the legal routes. The filter is the fence; controllers remain the handlers.

Do **not** add Spring Security for this. A filter is enough.

Suggested sketch (adapt package names):

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LegalPathFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (LegalPaths.isAllowed(request.getMethod(), request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        FriendlyErrorPages.write(response, 404, "Unknown or disallowed URL",
                request.getRequestURI());
    }
}
```

Centralize patterns in one `LegalPaths` (or equivalent) class so tests can cover allow/deny without spinning the full stack.

---

## 4. Friendly HTML instead of Whitelabel / empty browser errors

### 4.1 Turn off Whitelabel

```yaml
server:
  error:
    whitelabel:
      enabled: false
    include-message: never
```

Do not add Thymeleaf unless you already have it. Serve a small static HTML string or files under `src/main/resources/static/error/`.

### 4.2 When to return HTML vs JSON

| Request | Body |
|:--------|:-----|
| `POST /api/v1/shorten` validation / 400 / 503 | Keep **JSON** `ErrorResponse` (`GlobalExceptionHandler`) |
| `GET /short/{shortCode}` missing or expired | **HTML** 404 page (not JSON `ErrorResponse`) |
| Illegal path (filter) | **HTML** 404 page |
| No handler (if anything slips past the filter) | Same HTML 404 — custom `ErrorController` |
| App down / wrong port | Cannot serve HTML; start the app on 8080 |

Replace `ResourceNotFoundException` handling for redirect lookups so browsers get HTML. Do not use `@RestControllerAdvice` JSON for that case (or branch on `Accept` / path: `/short/**` → HTML, `/api/**` → JSON).

### 4.3 Page content (required tone)

A single simple page is enough (inline CSS is fine). Must include:

- Visible title, e.g. **Page not found**
- Short explanation: this URL is not a published route of the URL shortener, or the short code does not exist / has expired
- The requested path (escaped)
- Links to legal entry points: `POST /api/v1/shorten` (mention Swagger), `GET /swagger-ui.html`, `GET /actuator`

Must **not**:

- Say “Whitelabel Error Page”
- Show a raw Tomcat/Spring stack trace
- Return an empty 404 that makes Chrome show “This localhost page can’t be found”

Example structure:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <title>Page not found</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 40rem; margin: 3rem auto; color: #222; }
    a { color: #0b57d0; }
  </style>
</head>
<body>
  <h1>We could not find that page</h1>
  <p>The URL shortener does not serve <code>/the-path</code>.</p>
  <p>You can:</p>
  <ul>
    <li>Open the API docs at <a href="/swagger-ui.html">/swagger-ui.html</a></li>
    <li>Create a link with <code>POST /api/v1/shorten</code></li>
    <li>Follow a short link at <code>/short/{code}</code></li>
    <li>Check process health at <a href="/actuator">/actuator</a></li>
  </ul>
</body>
</html>
```

Escape the request URI in HTML (`<`, `&`, `"`).

### 4.4 Custom error controller (slip-net)

Implement `org.springframework.boot.web.servlet.error.ErrorController` on `/error` so container 404/500 still render the same HTML when `Accept` includes `text/html`. JSON clients calling `/api/**` may still receive `ErrorResponse`.

`/error` itself is an internal dispatcher path; do not advertise it as a legal user URL. The filter should allow `/error` so the error controller can run, or the filter writes HTML itself and never uses `/error`.

---

## 5. Tests (must update / add)

Keep existing cases; retarget paths. Add:

| Case | Expect |
|:-----|:-------|
| `GET /short/{activeCode}` | 302 + `Location` |
| `GET /short/unknown` | 404, `Content-Type` contains `text/html`, body contains a friendly heading, **not** “Whitelabel” |
| `GET /b9K2x1` (old path) | 404 HTML (illegal) |
| `GET /` or `GET /nope` | 404 HTML |
| `GET /api/v1/shorten` | 404 HTML (POST-only) |
| `POST /api/v1/shorten` valid | 201 JSON (unchanged) |
| `GET /swagger-ui.html` | 302 or 200 (Springdoc welcome) |
| `GET /v3/api-docs` | 200 JSON, paths include `/api/v1/shorten`, not `/ {shortCode}` |
| `GET /actuator` | 200 |

Do not `@Disabled` / `@Ignore`. `mvn test` must stay green.

---

## 6. File checklist (complete in one change)

**Move / path rewrite**

- [ ] `.ai/creation` → `.ai/greenfield`
- [ ] `AGENTS.md`, `.cursor/rules/*.mdc`, greenfield markdown that cites the old folder
- [ ] `.ai/brownfield/ADD_SWAGGER.md` (redirect path)

**Runtime**

- [ ] `RedirectController.java`
- [ ] `UrlShortenerService.java` (`shortUrl`)
- [ ] `application.yml` (disable Whitelabel)
- [ ] `LegalPaths` + `LegalPathFilter` (names may differ)
- [ ] Friendly HTML writer or `static/error/*.html` + `ErrorController`
- [ ] `GlobalExceptionHandler` (HTML for redirect 404)
- [ ] `OpenApiConfig` / DTO examples

**Tests**

- [ ] `UrlShortenerServiceTest.java`
- [ ] `UrlShortenerControllerIT.java`
- [ ] New filter / HTML 404 tests

No new Maven dependencies unless you already have a template engine. Prefer raw HTML.

No Spring Security, JWT, schema, or Redis as part of this refactor.

---

## 7. Smoke check

1. Start the app (`test` / H2 or `dev` / MySQL) on port **8080**.
2. `POST http://localhost:8080/api/v1/shorten` with a valid HTTPS URL → 201, `shortUrl` like `http://localhost:8080/short/xxxxx`.
3. `GET` that `shortUrl` → 302 to the long URL.
4. `GET http://localhost:8080/xxxxx` (no `/short/`) → HTML 404, not Whitelabel.
5. `GET http://localhost:8080/` → HTML 404.
6. `GET http://localhost:8080/swagger-ui.html` → Swagger UI.
7. `GET http://localhost:8080/v3/api-docs` → OpenAPI JSON.
8. `GET http://localhost:8080/actuator` → actuator index.

---

## 8. File name

This playbook is `.ai/brownfield/REFACTOR.md`.
