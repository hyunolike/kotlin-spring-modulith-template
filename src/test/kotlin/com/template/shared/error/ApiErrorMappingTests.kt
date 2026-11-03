package com.template.shared.error

import com.template.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester

/**
 * Spring MVC가 던지는 표준 예외가 500이 아니라 알맞은 4xx와 ApiResponse 형식으로 내려가는지 HTTP 레벨에서 검증한다.
 * 핸들러 메서드를 직접 호출하는 단위 테스트로는 예외 리졸버 우선순위 문제를 잡을 수 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ApiErrorMappingTests(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `존재하지 않는 경로는 404 NOT_FOUND로 응답한다`() {
        assertThat(mvc.get().uri("/api/v1/nope"))
            .hasStatus(HttpStatus.NOT_FOUND)
            .bodyJson()
            .extractingPath("$.error.code")
            .isEqualTo("NOT_FOUND")
    }

    @Test
    fun `지원하지 않는 HTTP 메서드는 405 METHOD_NOT_ALLOWED로 응답한다`() {
        assertThat(mvc.delete().uri("/api/v1/members/1"))
            .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
            .bodyJson()
            .extractingPath("$.error.code")
            .isEqualTo("METHOD_NOT_ALLOWED")
    }

    @Test
    fun `경로 변수 타입이 맞지 않으면 400 INVALID_REQUEST로 응답한다`() {
        assertThat(mvc.get().uri("/api/v1/members/abc"))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.error.code")
            .isEqualTo("INVALID_REQUEST")
    }

    @Test
    fun `필수 쿼리 파라미터가 없으면 400 INVALID_REQUEST로 응답한다`() {
        assertThat(mvc.get().uri("/api/v1/orders"))
            .hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.error.code")
            .isEqualTo("INVALID_REQUEST")
    }

    @Test
    fun `요청 본문 검증에 실패하면 400과 필드별 메시지로 응답한다`() {
        val result =
            mvc
                .post()
                .uri("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"","email":"not-an-email"}""")

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_REQUEST")
        assertThat(result)
            .bodyJson()
            .extractingPath("$.error.message")
            .asString()
            .contains("name", "email")
    }

    @Test
    fun `본문이 JSON이 아니면 400 INVALID_REQUEST로 응답한다`() {
        assertThat(
            mvc
                .post()
                .uri("/api/v1/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not json"),
        ).hasStatus(HttpStatus.BAD_REQUEST)
            .bodyJson()
            .extractingPath("$.error.code")
            .isEqualTo("INVALID_REQUEST")
    }

    @Test
    fun `오류 응답은 success=false이고 data가 없다`() {
        val result = mvc.get().uri("/api/v1/nope")

        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(false)
        assertThat(result).bodyJson().extractingPath("$.data").isNull()
    }
}
