# Refine — documentation, lineage, and leftover product gaps

Standalone playbook for further improvements after greenfield delivery and the brownfield passes (`ADD_SWAGGER.md`, `REFACTOR.md`, `ENHANCE.md`). It does not replace those files.

Do **not** add Redis, LangGraph inside the microservice, vector databases, or RAG to `UrlShortenerApplication`. `.notes/AI-Concept.ini` still applies: those sit in the developer-agent loop, not in the JVM service.

---

## Current gaps

| Area | Already in repo | Still missing or stale |
|:-----|:----------------|:------------------------|
| Specs and gates | `.ai/greenfield/` PRD, design, tests, manifest; `GATE2_APPROVAL.txt` | Ambiguous product choices (analytics, reliability bounds) are not written down as a decision log. |
| Brownfield | Swagger, `/short/{code}`, allowlist, HTML 404, Security + Jsoup demo | No one-page impact note (modules, APIs, data flow). |
| Orchestration | DAG in `ORCHESTRATION_MANIFEST.md`, audit log, metrics file, `AGENTS.md` | Manifest is policy. Gates 5–6 / Stage 7 incomplete (`stage1_epoch_ms`, `stage7_epoch_ms`, `end_to_end_latency_ms` are **null**; Docker image not built this run). Many lineage hashes are `n/a`. |
| Runnable service | Spring Boot 3.3, MySQL/H2, tests, JaCoCo, Swagger, allowlist, jsoup | `README.md` is one line. Coverage figures in the summary are stale. |
| Validation | Tests, cleanup job, 400/404/503, sanitizer | No compact risk register. |
| Autonomy | Gate 2 HITL, high-impact halt rules | No short human-vs-agent table. |
| Summary | `URL_SHORTENER_SUMMARY.md` | Omits brownfield, Security/jsoup, `/short/{code}`, HTML 404s; still Gate-4-era. |
| Scenarios | Greenfield + brownfield folders | Ambiguous-requirements write-up missing. |
| Architecture | `SYSTEM_DESIGN.md` (product) | No page that ties the DAG and gates to files that actually exist. |
| Analytics | 302 kept for a future counter | Click events, API, and retention never decided. |

Prefer artifacts that make the system runnable and the lifecycle visible:

1. README (how to run).
2. Orchestration architecture page + DAG tied to this repo.
3. Greenfield / brownfield / ambiguous write-ups.
4. Honest `URL_SHORTENER_SUMMARY.md` and filled metrics.
5. `mvn test` and, when practical, `docker compose`.

Finish those before extra product features.

---

## R1 — README and setup

Replace the one-line `README.md` with:

- What this repo is (gated SDLC workspace **plus** URL shortener service).
- JDK **17** (`JAVA_HOME`), Maven 3.9+, optional Docker.
- Tests: `mvn test` (JaCoCo ≥ 80%).
- Run: Cursor/VS Code **URL Shortener (test / H2)** or **dev / MySQL**, or `docker compose up --build`.
- URLs: `POST /api/v1/shorten`, `GET /short/{code}`, `/swagger-ui.html`, `/v3/api-docs`, `/actuator`.
- Pointers: `.ai/greenfield/`, `.ai/brownfield/`, `GATE2_APPROVAL.txt`, `SDLC_AUDIT_LOG.json`.
- Limits: lab passwords, sequential Base62, Security is a **permitAll** demo.

---

## R2 — Scenario write-ups

Add `.ai/scenarios/` (or equivalent sections in the summary). Each: intent → decomposition → lifecycle path → validation → outputs.

### R2a Greenfield (done — document it)

Point at `.ai/greenfield/` and Gate 2 → Stages 3–4. List parallel 3A/3B/3C and Gate 3 sync. Cite `mvn test`.

### R2b Brownfield (done — document it)

| Change | Modules | API / data |
|:-------|:--------|:-----------|
| Swagger | `pom`, `OpenApiConfig`, controllers/DTOs | `/swagger-ui.html`, `/v3/api-docs` |
| `/short/{code}` + allowlist + HTML 404 | `RedirectController`, `UrlShortenerService`, `LegalPathFilter` | Redirect path, `shortUrl` shape |
| Security + Jsoup demo | `SecurityConfig`, `HtmlSanitizeFilter` | 400 JSON on HTML `longUrl` |

Cite `ADD_SWAGGER.md`, `REFACTOR.md`, `ENHANCE.md`.

### R2c Ambiguous (missing)

Write `.ai/scenarios/AMBIGUOUS.md`: name each open choice and the decision. Optional small code follow-through.

| Open choice | Decision to record |
|:------------|:-------------------|
| Analytics with no event, API, or retention | **v1:** keep 302 so a later counter is possible; **no** click table in this pass. `GET /api/v1/stats/{code}` is a re-plan (schema + new human signature). |
| Unspecified reliability | **v1:** TTL, batched expiry delete, 503 on DB failure, H2 vs MySQL, compose healthcheck. |
| Orchestration as files vs an in-process graph runtime | **v1:** agent + `ORCHESTRATION_MANIFEST.md` + approval file + append-only audit/metrics. Not LangGraph inside the JAR. |
| Greenfield vs brownfield mix | `.ai/greenfield` vs `.ai/brownfield`. |
| Open create API vs locked-down auth | Demo Security `permitAll` + jsoup + allowlist; real auth stays out of band. |

If you implement one follow-through, pick **one** bounded item (example: `click_count` column + increment on successful redirect, **no** public stats API). Schema changes need a new human signature.

---

## R3 — Orchestration architecture page

Add `.ai/greenfield/ORCHESTRATION_ARCHITECTURE.md` (or top-level `ARCHITECTURE.md`):

- Diagram: operator ↔ agent ↔ workspace files ↔ Maven/Docker.
- DAG from the manifest **plus** what each gate produced here.
- Sequential vs parallel (Stage 3 A/B/C).
- Lineage: `SDLC_AUDIT_LOG.json` fields; how a PRD change would invalidate hashes (many hashes are still `n/a` — fix in R5).
- HITL: `GATE2_APPROVAL.txt`; high-impact list (schema, extra `pom` deps, coverage bar).
- Retry 3 / Safe-Stop / no `@Disabled`.
- Re-plan example: `GET /{code}` → `GET /short/{code}` after Gate 4.
- The shortener process is not an LLM. Agents write files; the JVM is deterministic.

`ORCHESTRATION_MANIFEST.md` stays the policy. This page must **tie gates to files**.

---

## R4 — Measurable governance

Update `SDLC_METRICS.json` with **real** values:

- `stage1_epoch_ms` / `stage7_epoch_ms` / `end_to_end_latency_ms` from the audit log, or leave Stage 7 incomplete **explicitly**.
- Refresh coverage from the latest `target/site/jacoco/jacoco.csv` (brownfield added lines; **123/134 is stale**).
- Keep `retry_rollback_frequency` and `mttr_ms` with the JDK 8 / Surefire `*IT` repair story.

Do not invent numbers. After `mvn test`, copy the JaCoCo line ratio into metrics.

If Stage 6 is still open: run `docker compose up --build` once and append the result to the audit log.

---

## R5 — Decision lineage

- SHA-256 of governing markdown (`PRD`, `TECH_STACK`, `TESTING_PLAN`, brownfield playbooks) on each new `SDLC_AUDIT_LOG.json` entry.
- New entries must not use `"n/a"` unless there is no upstream spec.
- One paragraph in the summary: how to grep the log for `Gate 2`, `REPAIR`, brownfield.

---

## R6 — Engineering summary

Rewrite `.ai/greenfield/URL_SHORTENER_SUMMARY.md`:

1. Plan and rationale (product and lifecycle).
2. Artifact manifest (greenfield + brownfield + gates + Docker + Swagger + Security demo).
3. Risks / trade-offs / validation (guessable codes, single MySQL, permitAll Security, HTML 404 vs JSON API, JDK 17 vs machine JDK 8).
4. Assumptions and limitations.
5. Pointers to the three scenario write-ups and the orchestration architecture page.

Gate 6 fails if the summary claims coverage or Docker that is not in the workspace.

---

## R7 — Human vs agent

Put this table in the summary or `ARCHITECTURE.md`:

| Action | Agent | Human |
|:-------|:------|:------|
| Method-body test repair | Yes | Review |
| New `pom` dependency / table | Halt | Signature file |
| Coverage bar &lt; 80% or `@Disabled` | Forbidden | — |
| Merge / release | — | Operator |

---

## R8 — Product extras (after R1–R6)

| Item | Bound it |
|:-----|:---------|
| Expose actuator `health` (and maybe `info`) | YAML `management.endpoints.web.exposure` |
| Click count on 302 (column + increment) | No public stats API unless R2c is written |
| `GET /short/{code}` regex `[0-9A-Za-z]{1,10}` | Match `LegalPaths`; do not collide with other routes |
| Compose + README walkthrough | One successful `up --build` |
| Rate limiting | Document as out of scope; do not add Redis |
| `ADD_SWAGGER.md` still says there is no Spring Security | One-line correction after `ENHANCE.md` |

Do **not** add custom aliases, login, JWT, Safe Browsing, Flyway, or Redis cache.

---

## R9 — Handling vague requirements

When a requirement is underspecified:

1. State the vague phrase.
2. List options.
3. Pick one and name the risk.
4. Append `SDLC_AUDIT_LOG.json` (`REQUIREMENTS` or `REPLAN`).
5. If schema or API changes, halt for a human signature.

---

## Suggested order

1. R1 README  
2. R2c ambiguous write-up  
3. R3 orchestration architecture  
4. R2a / R2b indexes  
5. R6 summary + R4 metrics from a fresh `mvn test`  
6. R5 hash discipline going forward  
7. Docker compose once if time  
8. R8 only if the above is solid  

---

## Out of scope

- LangChain / LangGraph inside the service process  
- Embedding models or MCP servers inside the JAR  
- Lowering JaCoCo or disabling tests  
- Real production passwords in Git  

---

## File name

This playbook is `.ai/brownfield/REFINE.md`. Apply items as separate diffs so decomposition stays visible.
