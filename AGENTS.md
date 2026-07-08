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

## Conventions

- REST: paths under `/api/v1`, every response wrapped in `ApiResponse<T>`
  (`shared/response`), errors via `BusinessException` + `ErrorCode`
  (`shared/error`) handled by `GlobalExceptionHandler`.
- Entities extend `BaseTimeEntity` (JPA auditing) and use table names that
  avoid SQL reserved words (`members`, `orders`).
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
- detekt runs with a pinned Kotlin version and ktlint is pinned to 1.6.0 in
  `build.gradle.kts` — do not remove those pins when bumping versions.
- Local compose maps PostgreSQL to host port **5433** (5432 is often taken);
  spring-boot-docker-compose auto-detects the mapped port.
- `docs/` is intentionally git-ignored (local working documents).
- CLAUDE.md is a symlink to this file — edit AGENTS.md only.
