# Automated Test Verification Suite Plan

Mandatory tests and the agent loop for Gate 4. The suite must pass `mvn clean test` at **100%** and JaCoCo **line coverage >= 80%**. Never use `@Ignore` or `@Disabled`.

Coverage is enforced by the JaCoCo plugin in `TECH_STACK.md`, not by a manual percentage in `URL_SHORTENER_SUMMARY.md`.

## 1. Unit Testing Matrix

### 1.1 `Base62EncoderTest.java`

No Spring context. Assert:

* **Pivot:** `encode(56800235L)` is alphanumeric `[0-9A-Za-z]+` and length **>= 5** (and **<= 7** for this pivot).
* **Alphabet:** encoded forms contain no `+` or `/`.
* **Max:** `encode(Long.MAX_VALUE)` completes without overflow; `decode(encode(Long.MAX_VALUE)) == Long.MAX_VALUE`.
* **Round-trip:** for a set of random positive longs, `decode(encode(id)) == id`.

### 1.2 `UrlShortenerServiceTest.java`

`@ExtendWith(MockitoExtension.class)`. Mock the JPA repository.

| Case | Setup | Expect |
|:-----|:------|:-------|
| Missing resource | Repo empty for short code | `ResourceNotFoundException` |
| Expired mapping | `expires_at` in the past | `ResourceNotFoundException` |
| Idempotent create | Active row for same `longUrl` | No second insert; return existing code |
| Default TTL | `ttlInDays` null | `expires_at` ≈ now + **730** days |
| Self-loop | `longUrl` host equals `APP_BASE_URL` host | validation failure / 400-path exception |
| Oversize URL | length > 2048 | validation failure |

## 2. Web Layer Integration Testing (`UrlShortenerControllerIT.java`)

Use `@SpringBootTest`, `@AutoConfigureMockMvc`, and **`@ActiveProfiles("test")`** so the datasource is **H2** from `TECH_STACK.md`. Seed data with the repository or `MockMvc` POST — **not** Mockito mocks.

A global `@ControllerAdvice` must serialize the error JSON from `PRD.md` / `SYSTEM_DESIGN.md`.

### 2.1 `POST /api/v1/shorten`

| Case | Assert |
|:-----|:-------|
| Valid HTTP(S) URL | **201**; body has `shortUrl`, `shortCode`, `expiresAt`; `shortCode` length 5–7 |
| Second POST of the same `longUrl` | **201** (or 200 if implemented as such — **prefer 201** with identical `shortCode`) |
| `"longUrl": "not-a-valid-url"` | **400** and error JSON `status`/`message` |
| Self-loop to `APP_BASE_URL` | **400** |

### 2.2 `GET /short/{shortCode}`

| Case | Setup | Assert |
|:-----|:------|:-------|
| Active mapping | Persist unexpired row (or create via POST) | **302** and `Location` equals `longUrl` |
| Unknown code | GET a code that was never inserted | **404** friendly HTML (not JSON, not Whitelabel) |
| Expired mapping | Persist row with `expires_at` in the past | **404** friendly HTML (HTTP layer, not only service unit) |

## 3. Automated Agent Workflow Cycle

JaCoCo `check` runs as part of `mvn clean test`. Do not skip tests (`-DskipTests`) except inside the Docker **image build** of a jar already verified at Gate 4.

```
[ Compile ] ──> [ mvn clean test  (Surefire + JaCoCo 80%) ]
                      │
         ┌────────────┴────────────┐
         ▼                         ▼
[ All tests pass + coverage ]   [ Failures or coverage < 80% ]
         │                         │
         ▼                         ▼
[ Gate 5: container phase ]   [ Read stack trace ]
         (Stage 6)                 │
                                   ▼
                          [ Patch failing class ]
                                   │
                                   ▼
                          [ Retry; max 3 times ]
                                   │
                    exhausted ──> [ Safe-Stop / rollback to Gate 2 ]
```

This loop **is** the Bounded Retry in `ORCHESTRATION_MANIFEST.md`. Passing tests do **not** rewrite classes. Gate 4C (secrets / parameterized SQL) is a compliance review of the same tree, not a JUnit case — still must pass before Stage 6.
