package com.template

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 * application-prod.yml이 실제로 적용된 컨텍스트를 검증한다.
 * DB 접속 정보는 Testcontainers(@ServiceConnection)가 대신하므로 환경 변수 자리만 채운다.
 */
@SpringBootTest(properties = ["DB_URL=unused", "DB_USERNAME=unused", "DB_PASSWORD=unused"])
@ActiveProfiles("prod")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ProdProfileTests(
    @Autowired private val mvc: MockMvcTester,
    @Autowired private val environment: Environment,
) {
    @Test
    fun `prod 프로필에서는 API 문서와 Swagger UI를 노출하지 않는다`() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatus(HttpStatus.NOT_FOUND)
        assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus(HttpStatus.NOT_FOUND)
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatus(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `prod 프로필은 로그를 한 줄 JSON(ECS)으로 남기도록 설정한다`() {
        // 실제 출력 형식은 여기서 검증하지 않는다: 로깅 시스템은 JVM에 하나라서 테스트 JVM에서는
        // 가장 먼저 뜬 컨텍스트의 설정이 유지되고, 실행 순서에 따라 결과가 달라진다.
        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("ecs")
    }

    @Test
    fun `prod 프로필에서도 헬스 체크는 열려 있다`() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatusOk()
    }
}
