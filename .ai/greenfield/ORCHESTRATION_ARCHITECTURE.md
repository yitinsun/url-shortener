# Orchestration architecture (this repo)

`ORCHESTRATION_MANIFEST.md` is the **policy**. This page maps that DAG onto **files that exist**.

The shortener JVM is not an LLM. Agents write the workspace; Maven and Docker execute deterministic artifacts.

```
operator
   │  GATE2_APPROVAL.txt  (HITL)
   ▼
agent (Cursor)  ──reads──►  .ai/greenfield  .ai/brownfield  AGENTS.md
   │                         writes src/, pom, tests, logs
   ▼
Maven (JDK 17) / optional Docker Compose
```

## DAG and what each gate produced

```
[1 REQUIREMENTS] ── Gate 1 ──► [2 ARCHITECTURE] ── Gate 2 HITL ──► [3 IMPLEMENTATION]
                                                                      3A ║ 3B ║ 3C
                                                                      └── Gate 3 sync ──►
[4–5 TEST / COMPLIANCE] ── Gate 4 ──► [6 CONTAINERS] ── Gate 5 ──► [7 RELEASE]
        │
        └── fail ──► FALLBACK (max 3) ──► Safe-Stop
upstream spec change ──► re-plan at Stage 1 (audit log kept)
```

| Gate | Meaning here | Artifact |
|:-----|:-------------|:---------|
| 1 | APIs and stack named | `.ai/greenfield/PRD.md` |
| 2 | Human unlock | `GATE2_APPROVAL.txt` (`[APPROVED: yitin]`) |
| 3 | Schema + encoder + Boot together | `schema.sql`, `Base62Encoder`, `pom.xml` / `src/main/java` |
| 4 | Tests + coverage + no string SQL | `mvn test`, JaCoCo check ≥ 0.80 |
| 5 | Compose/image | `Dockerfile`, `docker-compose.yml` present; **image build not recorded as done** |
| 6 | Honest summary | `.ai/greenfield/URL_SHORTENER_SUMMARY.md` |

Stage 3 ran **in parallel** (3A schema, 3B encoder, 3C Spring) then synced.

## Lineage

`SDLC_AUDIT_LOG.json` is append-only. Fields: `timestamp`, `stage`, `action_taken`, `upstream_dependency`, `upstream_dependency_hash`, `rationale`.

A change to `PRD.md` should invalidate downstream hashes and restart at Stage 1. Grep the log for `Gate 2`, `REPAIR`, `brownfield`, `REFACTOR`, `ENHANCE`.

New entries should SHA-256 the governing markdown. Older rows may still say `n/a`.

## Human vs agent

| Action | Agent | Human |
|:-------|:------|:------|
| Method-body test repair | Yes | Review |
| New `pom` dependency / SQL table | Halt | Signature file |
| Coverage bar &lt; 80% or `@Disabled` | Forbidden | — |
| Merge / release | — | Operator |

High-impact: schema after Gate 2, undeclared `pom` deps, lowering the coverage bar.

## Bounded repair

Max **3** test-repair loops, then Safe-Stop (freeze writes, do not start Stage 6). This repo used **one** repair: Surefire did not run `*IT` (JaCoCo &lt; 80%), then `**/*IT.java` was included. A JDK 8 `JAVA_HOME` compile failure was a separate environment miss, not a test skip.

## Re-plan example (brownfield)

After Gate 4, redirect moved from `GET /{shortCode}` to `GET /short/{shortCode}`. Tests, `shortUrl`, OpenAPI text, and greenfield PRD were updated in the same change (`REFACTOR.md`). That is non-linear relative to the first DAG, not a silent path-only patch.

## Metrics

`SDLC_METRICS.json`: compile success vs syntax failure, `retry_rollback_frequency`, `mttr_ms`, stage timestamps, coverage notes. Stage 7 end-to-end latency stays **unset** until a recorded container/release pass.
