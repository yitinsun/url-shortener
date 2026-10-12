# Agent governance (url-shortener)

This repo is run as a gated SDLC. Specs in `.ai/greenfield/` are **binding**. Do not invent APIs, dependencies, or coverage numbers.

## Before any work

1. Read `.ai/greenfield/INSTRUCTIONS.md` and `.ai/greenfield/ORCHESTRATION_MANIFEST.md`.
2. Use the other specs only for their role:
   - Product: `.ai/greenfield/PRD.md`
   - Design: `.ai/greenfield/SYSTEM_DESIGN.md`
   - Stack: `.ai/greenfield/TECH_STACK.md`
   - Tests: `.ai/greenfield/TESTING_PLAN.md`
   - Status: `.ai/greenfield/URL_SHORTENER_SUMMARY.md` (update only with real artifacts/metrics)

## Gate 2 lock (Stage 3)

Do **not** create or edit `pom.xml`, `src/**`, or other functional Java until the workspace root contains `GATE2_APPROVAL.txt` with a line `[APPROVED: HUMAN_ENGINEER_ID]`.

Allowed before approval: `.ai/greenfield/**`, `AGENTS.md`, `.cursor/rules/**`, `Dockerfile`, `docker-compose.yml`, audit/metrics files.

If the file is missing, refuse to implement and tell the operator to write the approval (and optionally `git tag gate2-approved`).

## After approval

- Stage 3: schema.sql (3A), Base62 (3B), Boot/JPA/controllers (3C). All three before tests.
- Stage 4–5: `TESTING_PLAN.md`. `mvn clean test`, JaCoCo ≥ 80%, no `@Disabled` / `@Ignore`. Max **3** repair retries, then Safe-Stop.
- Stage 6–7: containers + honest `URL_SHORTENER_SUMMARY.md`.
- Log every mutation in `SDLC_AUDIT_LOG.json`; update `SDLC_METRICS.json` (`INSTRUCTIONS.md`).

## High impact — stop and ask

Schema changes after Gate 2, `pom.xml` deps not listed in `TECH_STACK.md`, or lowering the 80% / mandatory test bar.

v1 out of scope: Redis, Flyway, Spring Security, rate limiting, Safe Browsing, custom aliases, auth, click APIs.
