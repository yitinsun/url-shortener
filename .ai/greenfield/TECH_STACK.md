# Technology Stack Profile: Spring Boot URL Shortener (Java 17 & MySQL)

This document is the **only** allowed toolchain for v1. Packages not listed here are high-impact (see `ORCHESTRATION_MANIFEST.md` §5.1).

**Explicitly out of v1 (do not add):** Redis, Spring Security, Resilience4j, Safe Browsing clients, Flyway/Liquibase. Schema is applied with `schema.sql` (Spring SQL init) plus the DDL in `SYSTEM_DESIGN.md`.

## 1. Core Tooling Versions

* **Language Runtime:** Java 17 LTS
* **Framework:** Spring Boot **3.3.x** (parent POM)
* **Build System:** Apache Maven 3.9+
* **Database Driver:** MySQL Connector/J 8.x
* **Test DB:** H2 (test scope only, MySQL compatibility mode)
* **Coverage:** JaCoCo Maven plugin, **minimum 80% line coverage**, `mvn clean test` fails below that bar

## 2. Application Dependency Suite (`pom.xml`)

Use `spring-boot-starter-parent` version `3.3.x`. Do not pin starter versions separately.

```xml
<properties>
    <java.version>17</java.version>
    <jacoco.version>0.8.12</jacoco.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Configure `jacoco-maven-plugin` so `mvn clean test` runs `prepare-agent` and a `check` rule: **LINE covered ratio ≥ 0.80**. Do not add other plugins without Gate 2 / high-impact approval.

## 3. Configuration Setup (`application.yml`)

All secrets and hosts use `${PROPERTY:DEFAULT}`. Defaults are **lab-only**. Never commit a literal production password.

`APP_BASE_URL` builds `shortUrl` in API responses.

```yaml
spring:
  application:
    name: url-shortener-service
  profiles:
    active: dev

app:
  base-url: ${APP_BASE_URL:http://localhost:8080}

---
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/${DB_NAME:shortener_dev}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
      continue-on-error: true
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

---
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/${DB_NAME:shortener_prod}?useSSL=true&serverTimezone=UTC&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: never
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

---
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:h2:mem:shortener;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
```

Integration tests use `@ActiveProfiles("test")` (or `src/test/resources/application-test.yml` matching the `test` profile). Do not point `@SpringBootTest` at production MySQL unless an operator explicitly runs a local MySQL profile.

`docker-compose.yml` must pass `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, and `APP_BASE_URL`. Compose may set lab passwords in `environment:`; Java and `application.yml` still only reference variables.

Prod schema (including `AUTO_INCREMENT=56800235`) is applied by DBA or a compose init mount of the same `schema.sql`, not by Hibernate `update`.

## 4. Development Constraints & Coding Standards

* **API types:** Request and response bodies on the HTTP boundary are immutable Java **`record`s**. Do not use Lombok `@Data` on those types.
* **JPA entities:** May use Lombok `@Getter` / `@Setter` or the allowed set below, plus `@NoArgsConstructor` / `@AllArgsConstructor` as JPA requires. Prefer explicit entities over `@Data` if it fights JPA defaults.
* **Database access:** Spring Data JPA repositories only. Native SQL strings built with `+` or formatted interpolation are banned. Parameterized `@Query` is allowed for batched expiry deletes.
* **Lombok allow-list:** `@Slf4j`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Getter`, `@Setter`. Do **not** use experimental Lombok (`@SuperBuilder`, `@With`). `@Data` and `@Builder` are **not** on the allow-list.
* **Scheduling:** Spring `@Scheduled` (already on the classpath via Boot) for expiry cleanup; no extra scheduler library.
