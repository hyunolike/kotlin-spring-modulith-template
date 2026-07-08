# Kotlin Spring Modulith Template

Kotlin + Spring Boot + [Spring Modulith](https://spring.io/projects/spring-modulith) 기반의
모듈러 모놀리스 템플릿입니다. 패키지 = 모듈 경계 규칙을 테스트로 강제하고,
모듈 간 통신은 파사드 인터페이스(동기)와 이벤트(비동기)로만 허용합니다.

## 기술 스택

- Kotlin 2.1 / Java 21 / Spring Boot 3.5 / Spring Modulith 1.4
- Spring Data JPA + PostgreSQL, Event Publication Registry(JPA)
- Testcontainers, springdoc-openapi, ktlint, detekt

## 시작하기

```bash
# Docker가 실행 중이어야 합니다. compose.yaml의 PostgreSQL이 자동 기동됩니다.
./gradlew bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- 테스트: `./gradlew test` (Testcontainers가 PostgreSQL 컨테이너를 띄웁니다)
- 린트: `./gradlew ktlintCheck detekt` / 자동 포맷: `./gradlew ktlintFormat`

## 패키지 구조

```
com.template
├── shared/    # 공유 모듈 (OPEN) — 공통 응답, 예외, 설정
├── member/    # 회원 모듈
└── order/     # 주문 모듈 (member에 단방향 의존)
```

각 모듈의 **루트 패키지만 다른 모듈에 노출**됩니다 (파사드 인터페이스, 공개 DTO, 이벤트).
`application` / `domain` / `presentation` 하위 패키지는 Spring Modulith가 자동으로 은닉하며,
다른 모듈에서 import하면 `ModularityTests`가 실패합니다.

```
member/
├── MemberApi.kt               # 파사드 인터페이스 (공개)
├── MemberInfo.kt              # 공개 DTO
├── MemberStatus.kt            # 공개 enum
├── MemberDeactivatedEvent.kt  # 공개 도메인 이벤트
├── application/               # 유스케이스 (은닉)
├── domain/                    # 엔티티, 리포지토리 (은닉)
└── presentation/              # 컨트롤러, 요청/응답 DTO (은닉)
```

## 모듈 간 통신 규칙

| 패턴 | 방법 | 예시 |
|---|---|---|
| 동기 호출 | 상대 모듈 루트의 파사드 인터페이스 | `OrderService` → `MemberApi.getMember()` |
| 비동기 통지 | 도메인 이벤트 + `@ApplicationModuleListener` | `MemberDeactivatedEvent` → order 모듈이 주문 취소 |

이벤트 소비자는 발행자의 이벤트 타입에 의존하므로, **동기 호출과 이벤트 소비는
같은 방향이어야 순환이 생기지 않습니다** (이 템플릿: order → member 단방향).

이벤트는 Event Publication Registry(`event_publication` 테이블)에 기록되어
리스너 실패 시 이력이 남고, `republish-outstanding-events-on-restart=true`로
재기동 시 미완료 이벤트를 재발행합니다.

## 새 모듈 추가 체크리스트

1. `com.template.<모듈명>` 패키지 생성
2. 모듈 루트에 공개 계약만 배치: 파사드 인터페이스, 공개 DTO, 이벤트
3. 구현은 `application` / `domain` / `presentation` 하위 패키지에 배치
4. 다른 모듈 접근은 파사드 호출 또는 이벤트 수신으로만 (순환 의존 금지)
5. `@ApplicationModuleTest` 모듈 테스트 작성 (의존 모듈 파사드는 `@MockitoBean`으로 대체)
6. `./gradlew test --tests "com.template.ModularityTests"` 로 경계 검증

## 모듈 문서 생성

`ModularityTests`의 문서 생성 테스트가 `build/spring-modulith-docs/`에
C4·PlantUML 다이어그램과 모듈 캔버스(AsciiDoc)를 생성합니다.

## 프로파일

- 기본(local): docker-compose 자동 연동, `ddl-auto: update`
- `prod`: `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` 환경변수 필요, `ddl-auto: validate`

## 참고 자료

- [Spring Modulith Reference](https://docs.spring.io/spring-modulith/reference/)
- [카카오뱅크 — 레거시에서 모듈러 모놀리스로](https://tech.kakaobank.com/posts/2507-legacy-to-modular-monolith-with-spring-modulith/)
- [team-dodn/spring-boot-kotlin-template](https://github.com/team-dodn/spring-boot-kotlin-template)
