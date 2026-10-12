# Greenfield scenario

**Intent:** build the URL shortener from specs with no existing Java module.

**Decomposition**

1. Requirements → `.ai/greenfield/PRD.md` (Gate 1: named APIs, Java 17, MySQL 8, Base62).
2. Architecture → `SYSTEM_DESIGN.md`, `TECH_STACK.md` (Gate 2: human `[APPROVED: yitin]` in `GATE2_APPROVAL.txt`).
3. Implementation in parallel (Gate 3 sync):
   - **3A** `src/main/resources/db/schema.sql`
   - **3B** `Base62Encoder` + unit tests
   - **3C** Boot module, JPA, controllers, handler
4. Tests / compliance (Gate 4): `mvn test`, JaCoCo ≥ 80%, parameterized JPA, no `@Disabled`.

**Orchestration path:** Stages 1→2→HITL→3A/3B/3C→4. Repair loop used once (Surefire omitted `*IT`, JaCoCo failed, include fixed; JDK 8 vs 17).

**Validation:** `UrlShortenerServiceTest`, `UrlShortenerControllerIT`, encoder and cleanup tests. Command: `mvn test` on JDK 17.

**Outputs:** `pom.xml`, `application.yml`, `src/main/java/**`, `GATE2_APPROVAL.txt`, audit/metrics files.

Policy: `.ai/greenfield/ORCHESTRATION_MANIFEST.md`. File mapping: `.ai/greenfield/ORCHESTRATION_ARCHITECTURE.md`.
