# Engineering Summary: URL Shortener Service

This file is the **Stage 7 closeout**. Until Gate 6, it records **current workspace truth**, not hoped-for metrics. Update it after a real `mvn clean test` and container run; never invent coverage percentages.

## 1. Status (as of spec alignment, pre-Stage 3)

| Item | State |
|:-----|:------|
| Lifecycle | Stages 1–2 documents aligned. **Stage 3 blocked on Gate 2** (`GATE2_APPROVAL.txt` with `[APPROVED: HUMAN_ENGINEER_ID]`). |
| Java / `pom.xml` / tests | **Not generated.** |
| `Dockerfile`, `docker-compose.yml` | Present (infra may exist before Gate 2). |
| `SDLC_AUDIT_LOG.json` / `SDLC_METRICS.json` | Not started (created when Stage 3 begins). |
| Measured coverage | **None.** Gate 4 requires >= 80% via JaCoCo after implementation. |

## 2. Plan & Engineering Rationale

Governed multi-agent SDLC (`ORCHESTRATION_MANIFEST.md`) building a **single-instance** URL shortener.

* **Design choice:** Deterministic **Base62(id)** bound to a MySQL `BIGINT` auto-increment key (seed **56800235**).
* **Rationale:** Uniqueness comes from the primary key, so inserts do not retry on hash collisions.
* **Idempotency** is a separate lookup of an **active** row by exact `long_url` (see `PRD.md`).

## 3. Artifact Manifest

| Artifact | Role | Present? |
|:---------|:-----|:---------|
| `.ai/creation/*.md` | Specs | Yes |
| `Dockerfile`, `docker-compose.yml` | Prod profile + MySQL 8.0 | Yes |
| `pom.xml`, `application.yml` | Boot 3.3.x / Java 17 / JPA | No — Stage 3C |
| `src/main/resources/db/schema.sql` | Official DDL | No — Stage 3A |
| `Base62Encoder.java` / `Base62EncoderTest.java` | Encode/decode | No — Stage 3B |
| Controllers, service, JPA entity, `@ControllerAdvice` | HTTP + domain | No — Stage 3C |
| `UrlShortenerServiceTest.java`, `UrlShortenerControllerIT.java` | Gate 4 | No |

After Gate 4, replace this table with paths that exist and attach the JaCoCo report figure.

## 4. Risks, Trade-offs, & Validation

* **MySQL vs Redis:** v1 chooses durability and simpler operations over in-memory lookup speed. Redirect **< 20 ms** is a design target (unique `short_code` index), not a load-test gate.
* **Guessable codes:** sequential Base62 is accepted in v1; salting is a re-plan.
* **Single database:** no replica, no shard, no load balancer in compose.
* **Validation:** report only numbers produced by `mvn clean test`. Do not copy placeholder values (for example 88.4%) without a JaCoCo XML/HTML report in the tree.

## 5. Assumptions & Limitations

**Assumptions**

* ID seed `56800235` so the first live codes are **at least 5** characters.
* Traffic is read-heavy (~100:1).
* Lab defaults `${DB_PASSWORD:changeme}` are not production secrets.

**Limitations**

* Single MySQL instance is a **write bottleneck** and SPOF.
* No sharding / multi-region.
* No rate limiter, Safe Browsing, custom aliases, auth, or click analytics in v1.
* H2 is used for automated ITs; production-like checks use MySQL 8.0 via compose.
