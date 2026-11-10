# AGENTS.md

Guidance for AI coding agents working in this repository.

## Project Overview

Kotlin + Spring Boot + Spring Modulith modular monolith template.
Single Gradle module; top-level packages under `com.template` are the
Modulith modules: `shared` (OPEN), `member`, `order`.

## Commands

```bash
./gradlew test                                              # all tests (requires Docker for Testcontainers)
./gradlew test --tests "com.template.ModularityTests"       # module boundary verification only
./gradlew ktlintCheck detekt                                # lint & static analysis
./gradlew ktlintFormat                                      # auto-format (run before ktlintCheck on new code)
./gradlew bootRun                                           # local run (starts PostgreSQL via compose.yaml)
./gradlew clean build                                       # full verification before finishing work
docker build -t kotlin-spring-modulith-template .           # container image (prod profile, no tests inside)
```

## Architecture Rules (enforced by tests — do not break)

- **Package = module boundary.** Only a module's root package is visible to
  other modules (facade interfaces, public DTOs, events). `application`,
  `domain`, `presentation` sub-packages are hidden by Spring Modulith.
- **No dependency cycles between modules.** An event consumer compiles
  against the publisher's event type, so synchronous facade calls and event
  consumption must point in the same direction. In this repo the direction is
  strictly `order → member`; `member` must never reference `order`.
- Cross-module access happens only through a facade (e.g. `MemberApi`) or an
  event listener (`@ApplicationModuleListener`). Never inject another
  module's repository, service, or entity.
- `ApplicationModules.verify()` in `ModularityTests` fails the build on any
  violation. Fix the code, never relax the verification.
- The same verification also runs at application startup
  (`spring-modulith-runtime` + `spring.modulith.runtime.verification-enabled`)
  — a violation prevents the app from booting.

## Conventions

- REST: paths under `/api/v1`, every response wrapped in `ApiResponse<T>`
  (`shared/response`), errors via `BusinessException` + `ErrorCode`
  (`shared/error`) handled by `GlobalExceptionHandler`. A success with nothing
  to return uses `ApiResponse.success()` (`data: null`), never `success(Unit)`.
- Request DTOs mirror the column definitions they are stored in: `@Size(max = …)`
  for every `varchar`, `@Digits` for every `numeric`. Without them an oversized
  value surfaces as a 500 from the database, and an over-precise decimal is
  silently rounded on write.
- Entities extend `BaseTimeEntity` (JPA auditing, `Instant` → `timestamptz`) and
  use table names that avoid SQL reserved words (`members`, `orders`). Timestamps
  are never `LocalDateTime`: a zone-less column drifts when the app, DB and
  analytics hosts run in different zones.
- Every schema change ships as a Flyway migration next to the entity change,
  in the owning module's folder: `db/migration/<module>/V{n}__*.sql`
  (`spring.modulith.runtime.flyway-enabled`). Each module has its own version
  sequence and history table (`flyway_schema_history_<module>`). A migration
  never touches another module's tables — no cross-module foreign keys. The
  Event Publication Registry tables belong to `shared`.
- List endpoints take `PageQuery` (`?page=&size=`, size capped at 100) and
  return `PageResponse<T>` inside `ApiResponse`. Sorting is fixed per endpoint
  in code; never accept Spring Data `Pageable` (client-controlled `sort`)
  directly in a controller.
- Mutable entities carry a `@Version` column (optimistic locking); the
  resulting `OptimisticLockingFailureException` maps to 409
  `CONCURRENT_MODIFICATION`. A pre-check like `existsByEmail` is not race-safe —
  back it with a DB unique constraint and translate
  `DataIntegrityViolationException` into the matching `BusinessException`, but
  only after checking which constraint failed (`violatesConstraint(name)` in
  `shared/error`): the same exception also covers length and not-null violations.
  Values compared case-insensitively (e-mail) are normalized before the check.
- When a module writes based on another module's state (e.g. `order` placing
  an order only for an ACTIVE member), read that state through a locking
  facade method (`MemberApi.getMemberWithSharedLock`, `FOR SHARE`,
  `Propagation.MANDATORY`). A plain read lets the state change commit before
  the write does, and the change event's listener then misses the new row.
- Async work (`@ApplicationModuleListener`) inherits the request's MDC via
  `MdcTaskDecorator` and its trace context via `ContextPropagatingTaskDecorator`
  (`TracingConfig`), so logs keep the same `requestId` and `traceId`.
- Logging is configured through `logging.*` properties only. Do not add a
  `logback-spring.xml`: a custom appender bypasses `logging.structured.format.*`
  (JSON logs in `prod`) and the correlation pattern.
- Event listeners must be idempotent. A failed publication stays `FAILED` in the
  registry and `EventPublicationMaintenance` (`shared/config`) resubmits it on a
  schedule, up to `template.events.resubmission.max-attempts` tries in total;
  stuck ones are failed by `spring.modulith.events.staleness.*`. Restart-time
  republishing is off on purpose: it would replay events another instance is
  still processing.
- Global infrastructure annotations (`@EnableAsync`, `@EnableJpaAuditing`,
  `@EnableScheduling`)
  live on `TemplateApplication`, not in a module — `@ApplicationModuleTest`
  bootstraps a single module and would miss module-local config.
- Module tests use `@ApplicationModuleTest` + `TestcontainersConfiguration`;
  mock dependency facades with `@MockitoBean`; assert event flows with the
  `Scenario` DSL. Test names are Korean backtick sentences.
- Tests never share data: `DatabaseCleanupListener` (registered in
  `src/test/resources/META-INF/spring.factories`) truncates every table before
  each test method. Do not rely on rows from another test or on test order.
- Kotlin style is ktlint `ktlint_official`: no blank line at the start of a
  class body, explicit imports (no wildcards), trailing commas, max line 120.
  `.editorconfig` carries the same settings for IDE formatting.

## Gotchas

- Kotlin can't express package-level annotations: module metadata such as
  `@ApplicationModule(type = OPEN)` lives in `src/main/java/**/package-info.java`.
  Each module's `package-info.java` Javadoc doubles as the module description
  in Documenter-generated docs (Modulith 2.0+).
- Modulith config properties live under `spring.modulith.events.*` /
  `spring.modulith.runtime.*` — the bare `spring.modulith.republish-…` path is
  deprecated since 1.3.
- detekt and ktlint each run with a pinned Kotlin version in `build.gradle.kts`
  (ktlint 1.7.1 is built against Kotlin 2.2.0, see `ktlintKotlinVersion`). Do not
  remove those pins when bumping versions; bump ktlint and its Kotlin pin together.
  Without the pin, the project Kotlin version leaks into the ktlint classpath and
  every ktlint task fails with "Extensions storage is not registered".
- Local compose maps PostgreSQL to host port **5433** (5432 is often taken);
  spring-boot-docker-compose auto-detects the mapped port.
- Schema is owned by Flyway (`src/main/resources/db/migration/<module>/V{n}__*.sql`)
  and `ddl-auto` is `validate` in every profile — an entity change without a
  matching migration fails at startup (and in every Testcontainers test). Never
  edit an applied migration; add a new version. A local DB created before the
  per-module layout (single `V1__init.sql`), or one with an edited migration,
  fails at startup — reset it with `docker compose down -v`.
- `shared` is declared as a shared module (`@Modulithic(sharedModules = …)`), so
  `@ApplicationModuleTest` always bootstraps it together with the module under
  test, including its migrations. Modulith skips `db/migration/__root` in module
  tests, so nothing that module tests need may live there.
- Spring Boot turns tracing off in tests; a test that asserts trace IDs needs
  `@AutoConfigureTracing`. The log format in tests is whatever the first loaded
  context configured (logging is JVM-wide), so do not assert on log layout.
- `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` so Spring MVC
  standard exceptions (404/405/400) keep their status instead of becoming 500. Do
  not add `@ExceptionHandler` for an exception the parent already handles
  (ambiguous mapping breaks startup) — change the body in `handleExceptionInternal`.
- Testcontainers tests need a UTF-8 locale (`LANG=C.UTF-8`) because Korean test
  names become report file names.
- `docs/` is intentionally git-ignored (local working documents).
- CLAUDE.md is a symlink to this file — edit AGENTS.md only.
