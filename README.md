# url-shortener

Gated SDLC workspace and a Spring Boot **URL shortener** service (Java 17, MySQL 8 / H2 for tests).

Agents follow `.ai/greenfield/` (build from spec) and `.ai/brownfield/` (later changes). The running process is ordinary Spring Boot: no LLM inside the JAR.

## Requirements

- JDK **17** (`JAVA_HOME`, e.g. `C:\Program Files\Java\jdk-17.0.1`)
- Apache Maven 3.9+
- Optional: Docker Desktop (compose)

ATTENTION: Set `JAVA_HOME` to 17 before Maven.

## Tests

```bash
mvn test
```

Surefire runs `*Test` and `*IT`. JaCoCo fails the build if line coverage is below **80%**. Do not use `@Disabled` / `@Ignore`.

## Run the app

**H2 (test profile)** — Cursor/VS Code launch: **URL Shortener (test / H2)**  
or:

```bash
mvn -DskipTests spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.useTestClasspath=true
```

**MySQL (dev profile)** — launch **URL Shortener (dev / MySQL)** after MySQL is up (`shortener_dev`, see `application.yml` / launch env).

**Compose (prod profile + MySQL 8):**

```bash
docker compose up --build
```

Listens on **8080**.

## HTTP surface

| Method | Path | Notes |
|:-------|:-----|:------|
| POST | `/api/v1/shorten` | JSON body `{ "longUrl", "ttlInDays?" }` → 201 |
| GET | `/short/{shortCode}` | 302 `Location` to the long URL |
| GET | `/swagger-ui.html` | OpenAPI UI |
| GET | `/v3/api-docs` | OpenAPI JSON |
| GET | `/actuator` | Actuator index (`health`, `info`) |

Anything else is rejected with a friendly HTML 404 (not Whitelabel). `shortUrl` in create responses is `{APP_BASE_URL}/short/{shortCode}`.

## Layout

| Path | Role |
|:-----|:-----|
| `.ai/greenfield/` | Specs, DAG, tests plan, engineering summary |
| `.ai/brownfield/` | Swagger, refactor, security/jsoup demo, refine |
| `.ai/scenarios/` | Greenfield / brownfield / ambiguous write-ups |
| `GATE2_APPROVAL.txt` | Human unlock for implementation |
| `SDLC_AUDIT_LOG.json` | Append-only decisions |
| `SDLC_METRICS.json` | Repair / coverage telemetry |

## Limits

- Lab DB passwords via `${DB_PASSWORD:…}` / compose env — not production secrets.
- Short codes are sequential Base62(id); they are guessable.
- Spring Security is a **permitAll** demo (CSRF off, no login). Rate limiting, Redis, custom aliases, and JWT are out of v1.
