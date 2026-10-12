# Engineering Summary: URL Shortener Service

## 1. Status

| Item | State |
|:-----|:------|
| Lifecycle | Gate 2 approved (`GATE2_APPROVAL.txt` = `[APPROVED: yitin]`). Stages 3–4 complete. Stage 6 compose files exist; **image not built this run**. Stage 7 incomplete. |
| Java / `pom.xml` / tests | Present. `mvn test` green on JDK 17 (`C:\Program Files\Java\jdk-17.0.1`). |
| `Dockerfile`, `docker-compose.yml` | Present (`APP_BASE_URL` + schema init on MySQL). |
| `SDLC_AUDIT_LOG.json` / `SDLC_METRICS.json` | Written. `stage1_epoch_ms` set from first audit timestamp. `stage7_epoch_ms` and `end_to_end_latency_ms` remain **null**. |
| Measured coverage | **268 / 299 lines (89.63%)** from `target/site/jacoco/jacoco.csv` after `mvn test`. JaCoCo check ≥ 80% passed. |

## 2. Plan & Engineering Rationale

**Product:** HTTP API that stores a long URL and returns a short code. Redirect uses **302** so a later click counter can be added without changing the redirect status. v1 identity is MySQL `BIGINT` with seed **56800235**; short codes are **Base62(id)** (not a hash of the URL).

**Lifecycle:** Specs live in `.ai/greenfield/`. Agents follow `ORCHESTRATION_MANIFEST.md` and `AGENTS.md`. Gate 2 is a human file. Stages 3A (schema), 3B (encoder), and 3C (Boot/JPA) must exist before `mvn test`. Brownfield playbooks (`ADD_SWAGGER.md`, `REFACTOR.md`, `ENHANCE.md`, `REFINE.md`) describe later diffs; they do not replace the PRD.

**Idempotency:** Re-shortening an exact `longUrl` that already has an **active** row returns that row.

**H2 ITs:** First identity values are small; the service left-pads Base62 to **5** characters so IT length rules hold without changing `encode()` math.

**Ambiguous choices** (recorded, not invented later): see `.ai/scenarios/AMBIGUOUS.md`. Analytics stay 302-only (no `click_count` column this pass — schema needs a new human signature). Orchestration is policy files + agent, not LangGraph in the JAR.

## 3. Artifact Manifest

| Artifact | Present? |
|:---------|:---------|
| `GATE2_APPROVAL.txt` | Yes |
| `.ai/greenfield/` PRD, design, stack, tests, manifest, instructions | Yes |
| `.ai/greenfield/ORCHESTRATION_ARCHITECTURE.md` | Yes |
| `.ai/scenarios/{GREENFIELD,BROWNFIELD,AMBIGUOUS}.md` | Yes |
| `.ai/brownfield/` ADD_SWAGGER, REFACTOR, ENHANCE, REFINE | Yes |
| `README.md` | Yes (setup, URLs, limits) |
| `pom.xml`, `application.yml` | Yes |
| `src/main/resources/db/schema.sql` | Yes (no click_count) |
| `Base62Encoder.java` / `Base62EncoderTest.java` | Yes |
| Controllers, service, JPA, `@ControllerAdvice`, friendly HTML 404 | Yes |
| `GET /short/{shortCode:[0-9A-Za-z]{1,10}}` | Yes |
| Springdoc `/swagger-ui.html`, `/v3/api-docs` | Yes |
| `SecurityConfig` (permitAll demo), `LegalPathFilter`, `HtmlSanitizeFilter` (jsoup) | Yes |
| Actuator `health`,`info` | Yes (`management.endpoints.web.exposure.include`) |
| `UrlShortenerServiceTest.java`, `UrlShortenerControllerIT.java` | Yes |
| `Dockerfile`, `docker-compose.yml` | Yes (compose **not** executed this run) |

## 4. Risks, Trade-offs, & Validation

* MySQL over Redis: durability over in-memory speed. Rate limiting / cache stay out of scope.
* Sequential Base62 codes are guessable (accepted v1).
* Single MySQL instance in compose.
* Spring Security is a **permitAll** demo; it does not authenticate callers.
* Browser 404s are HTML; API errors remain JSON.
* Default `JAVA_HOME` on this machine is JDK 8 — Maven must use JDK 17.
* **Validation:** `mvn test` (Surefire + JaCoCo ≥ 80%) passed. Docker image / compose up **not** claimed.

## 5. Assumptions & Limitations

* Default TTL 730 days; lab password default `${DB_PASSWORD:changeme}`.
* Compose still uses `prod_password` in container env (not in Java source).
* No rate limiter, Safe Browsing, custom aliases, JWT, Flyway, or public stats API.
* Click events and retention are undecided; do not add a click table without a new signature.

## 6. Human vs agent

| Action | Agent | Human |
|:-------|:------|:------|
| Method-body test repair | Yes | Review |
| New `pom` dependency / table | Halt | Signature file |
| Coverage bar &lt; 80% or `@Disabled` | Forbidden | — |
| Merge / release | — | Operator |

## 7. Lineage (how to read the audit log)

`SDLC_AUDIT_LOG.json` is append-only. Grep `Gate 2` / `GATE2` for the approval that unlocked `src/**`. Grep `REPAIR` for the JDK 8 and Surefire `*IT` recovery. Grep `ADD_SWAGGER`, `REFACTOR`, `ENHANCE`, `REFINE` for brownfield. New entries carry SHA-256 of the governing markdown (not `"n/a"` when a spec exists).

A PRD or schema change after Gate 2 invalidates downstream hashes and requires a re-plan plus a new human signature for high-impact items.

## 8. Scenario and architecture pointers

* Greenfield path: `.ai/scenarios/GREENFIELD.md`
* Brownfield path: `.ai/scenarios/BROWNFIELD.md`
* Ambiguous decisions: `.ai/scenarios/AMBIGUOUS.md`
* DAG tied to files: `.ai/greenfield/ORCHESTRATION_ARCHITECTURE.md`
