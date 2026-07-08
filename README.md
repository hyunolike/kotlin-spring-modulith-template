<div align="center">

# Kotlin Spring Modulith Template

**A production-ready modular monolith template built with Kotlin, Spring Boot, and Spring Modulith**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Modulith](https://img.shields.io/badge/Spring%20Modulith-1.4-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-modulith)
[![JDK](https://img.shields.io/badge/JDK-21-437291?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org)

**English** | [한국어](README.ko.md)

</div>

---

Package boundaries **are** module boundaries — and they are enforced by tests.
Modules talk to each other only through facade interfaces (sync) and domain
events (async), so you get microservice-grade boundaries with monolith-grade
simplicity.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Module Communication](#module-communication)
- [Adding a New Module](#adding-a-new-module)
- [Testing](#testing)
- [Configuration Profiles](#configuration-profiles)
- [References](#references)

## Features

- 🧱 **Enforced module boundaries** — `ApplicationModules.verify()` fails the build on any boundary violation or dependency cycle
- 🔄 **Two communication patterns out of the box** — synchronous facade calls and asynchronous domain events, demonstrated with working `member`/`order` modules
- 📬 **Reliable eventing** — Event Publication Registry persists every event to the `event_publication` table and republishes incomplete ones on restart
- 🧪 **Module-level testing** — `@ApplicationModuleTest` bootstraps one module at a time, with the `Scenario` DSL for event flows and Testcontainers for PostgreSQL
- 📐 **Living architecture docs** — C4 / PlantUML diagrams and module canvases generated from code by the Modulith `Documenter`
- 🛡️ **Consistent API surface** — global exception handling, unified `ApiResponse<T>` envelope, request-ID (MDC) logging, Swagger UI
- 🧹 **Code quality gates** — ktlint and detekt wired into the build
- 🐳 **Zero-setup local run** — `compose.yaml` + spring-boot-docker-compose starts PostgreSQL automatically

## Getting Started

### Prerequisites

- Docker (for local PostgreSQL and Testcontainers)
- JDK 21 — auto-provisioned by the Gradle toolchain if missing

### Run

```bash
./gradlew bootRun
```

PostgreSQL from `compose.yaml` starts automatically. Then visit:

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

### Try the API

```bash
# Register a member
curl -X POST localhost:8080/api/v1/members \
  -H 'Content-Type: application/json' \
  -d '{"name":"Jane","email":"jane@example.com"}'

# Place an order (order module validates the member via MemberApi)
curl -X POST localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"memberId":1,"productName":"Keyboard","amount":120000}'

# Deactivate the member → MemberDeactivatedEvent → orders are cancelled asynchronously
curl -X POST localhost:8080/api/v1/members/1/deactivate
curl "localhost:8080/api/v1/orders?memberId=1"   # status: CANCELLED
```

## Project Structure

```
com.template
├── TemplateApplication.kt     # Root: global infra config (@EnableAsync, @EnableJpaAuditing)
├── shared/                    # Shared module (OPEN) — common response, errors, config
│   ├── response/              #   ApiResponse, ErrorResponse
│   ├── error/                 #   ErrorCode, BusinessException, GlobalExceptionHandler
│   ├── domain/                #   BaseTimeEntity (JPA auditing)
│   └── config/                #   OpenAPI config, MDC logging filter
├── member/                    # Member module
│   ├── MemberApi.kt           #   Facade interface        (public)
│   ├── MemberInfo.kt          #   Public DTO              (public)
│   ├── MemberStatus.kt        #   Public enum             (public)
│   ├── MemberDeactivatedEvent.kt  # Domain event          (public)
│   ├── application/           #   Use cases               (hidden)
│   ├── domain/                #   Entity, repository      (hidden)
│   └── presentation/          #   Controller, DTOs        (hidden)
└── order/                     # Order module (one-way dependency on member)
    ├── OrderInfo.kt / OrderStatus.kt
    ├── application/           #   OrderService, MemberEventListener
    ├── domain/
    └── presentation/
```

Only the **root package of each module is visible** to other modules (facade
interfaces, public DTOs, events). The `application` / `domain` /
`presentation` sub-packages are hidden by Spring Modulith's default rules —
importing them from another module fails `ModularityTests`.

## Module Communication

| Pattern | How | Example in this template |
|---|---|---|
| Synchronous call | Facade interface in the target module's root | `OrderService` → `MemberApi.getMember()` |
| Asynchronous notification | Domain event + `@ApplicationModuleListener` | `MemberDeactivatedEvent` → order module cancels the member's orders |

> **Rule of thumb:** an event consumer compiles against the publisher's event
> type. To stay cycle-free, synchronous calls and event consumption must point
> in the **same direction** — in this template, strictly `order → member`.

Events are persisted in the Event Publication Registry (`event_publication`
table). If a listener fails, the record remains incomplete, and
`republish-outstanding-events-on-restart=true` replays it on the next startup.

## Adding a New Module

1. Create a `com.template.<module>` package
2. Put only the public contract in the module root: facade interface, public DTOs, events
3. Implement inside `application` / `domain` / `presentation` sub-packages
4. Reach other modules only via facades or events — never create cycles
5. Write an `@ApplicationModuleTest` (mock dependency facades with `@MockitoBean`)
6. Run `./gradlew test --tests "com.template.ModularityTests"` to verify boundaries

## Testing

```bash
./gradlew test                 # all tests (Testcontainers spins up PostgreSQL)
./gradlew ktlintCheck detekt   # lint & static analysis
./gradlew ktlintFormat         # auto-format
```

- `ModularityTests` — verifies module boundaries and generates architecture
  docs (C4 / PlantUML + module canvases) into `build/spring-modulith-docs/`
- `MemberModuleTests` / `OrderModuleTests` — module-scoped tests using the
  `Scenario` DSL to assert event publication and consumption

## Configuration Profiles

| Profile | Database | DDL | Notes |
|---|---|---|---|
| default (local) | auto-started via docker compose | `update` | Swagger UI, SQL logging enabled |
| `prod` | `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars | `validate` | use a migration tool (e.g. Flyway) |

## References

- [Spring Modulith Reference Documentation](https://docs.spring.io/spring-modulith/reference/)
- [KakaoBank — From Legacy to Modular Monolith with Spring Modulith](https://tech.kakaobank.com/posts/2507-legacy-to-modular-monolith-with-spring-modulith/) (Korean)
- [team-dodn/spring-boot-kotlin-template](https://github.com/team-dodn/spring-boot-kotlin-template)
