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
  (`shared/error`) handled by `GlobalExceptionHandler`.
- Entities extend `BaseTimeEntity` (JPA auditing) and use table names that
  avoid SQL reserved words (`members`, `orders`).
- Mutable entities carry a `@Version` column (optimistic locking); the
  resulting `OptimisticLockingFailureException` maps to 409
  `CONCURRENT_MODIFICATION`. A pre-check like `existsByEmail` is not race-safe —
  back it with a DB unique constraint and translate
  `DataIntegrityViolationException` into the matching `BusinessException`.
- When a module writes based on another module's state (e.g. `order` placing
  an order only for an ACTIVE member), read that state through a locking
  facade method (`MemberApi.getMemberWithSharedLock`, `FOR SHARE`,
  `Propagation.MANDATORY`). A plain read lets the state change commit before
  the write does, and the change event's listener then misses the new row.
- Async work (`@ApplicationModuleListener`) inherits the request's MDC via
  `MdcTaskDecorator`, so logs keep the same `requestId`.
- Global infrastructure annotations (`@EnableAsync`, `@EnableJpaAuditing`)
  live on `TemplateApplication`, not in a module — `@ApplicationModuleTest`
  bootstraps a single module and would miss module-local config.
- Module tests use `@ApplicationModuleTest` + `TestcontainersConfiguration`;
  mock dependency facades with `@MockitoBean`; assert event flows with the
  `Scenario` DSL. Test names are Korean backtick sentences.
- Kotlin style is ktlint `ktlint_official`: no blank line at the start of a
  class body, explicit imports (no wildcards), trailing commas, max line 120.

## Gotchas

- Kotlin can't express package-level annotations: module metadata such as
  `@ApplicationModule(type = OPEN)` lives in `src/main/java/**/package-info.java`.
  Each module's `package-info.java` Javadoc doubles as the module description
  in Documenter-generated docs (Modulith 2.0+).
- Modulith config properties live under `spring.modulith.events.*` /
  `spring.modulith.runtime.*` — the bare `spring.modulith.republish-…` path is
  deprecated since 1.3.
- detekt runs with a pinned Kotlin version and ktlint is pinned to 1.7.1 in
  `build.gradle.kts` — do not remove those pins when bumping versions.
- Local compose maps PostgreSQL to host port **5433** (5432 is often taken);
  spring-boot-docker-compose auto-detects the mapped port.
- Adding a non-null column (e.g. `@Version`) under `ddl-auto: update` can fail on
  a local DB that already has rows — reset it with `docker compose down -v`.
- Testcontainers tests need a UTF-8 locale (`LANG=C.UTF-8`) because Korean test
  names become report file names.
- `docs/` is intentionally git-ignored (local working documents).
- CLAUDE.md is a symlink to this file — edit AGENTS.md only.
