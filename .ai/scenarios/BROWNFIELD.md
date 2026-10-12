# Brownfield scenario

**Intent:** change a running service without skipping the allowlist, tests, or error pages.

**Decomposition:** playbook in `.ai/brownfield/` → impact table → code/tests → `mvn test`.

| Change | Playbook | Modules | API / data |
|:-------|:---------|:--------|:-----------|
| OpenAPI UI | `ADD_SWAGGER.md` | `pom`, `OpenApiConfig`, controllers/DTOs | `/swagger-ui.html`, `/v3/api-docs` |
| Redirect path, allowlist, HTML 404 | `REFACTOR.md` | `RedirectController`, `UrlShortenerService`, `LegalPathFilter`, `FriendlyErrorPages` | `GET /short/{code}`, `shortUrl` prefix `/short/` |
| Security + Jsoup demo | `ENHANCE.md` | `SecurityConfig`, `HtmlSanitizeFilter` | 400 JSON if `longUrl` has HTML |
| Specs folder rename | `REFACTOR.md` | `.ai/creation` → `.ai/greenfield` | Docs only |
| Docs / metrics leftovers | `REFINE.md` | README, scenarios, architecture, summary | No schema |

**Orchestration path:** post–Gate 4, non-linear. Example re-plan: `GET /{shortCode}` → `GET /short/{shortCode}` after tests already existed.

**Validation:** ITs for 302 on `/short/{code}`, HTML 404 on `/` and old `/{code}`, OpenAPI JSON, jsoup 400, actuator. Still `mvn test`.

**Outputs:** filters, Security config, springdoc, friendly HTML, updated tests and greenfield docs.
