package com.template

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.assertj.MvcTestResult

/**
 * 요청 값이 DB 컬럼 정의를 벗어나면 DB 오류(500)나 엉뚱한 오류 코드가 아니라 400으로 거절되는지 HTTP 레벨에서 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ApiValidationTests(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `이름이 255자를 넘으면 DUPLICATE_EMAIL이 아니라 400 INVALID_REQUEST로 응답한다`() {
        val result = post("/api/v1/members", """{"name":"${"a".repeat(256)}","email":"long-name@example.com"}""")

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_REQUEST")
        assertThat(result)
            .bodyJson()
            .extractingPath("$.error.message")
            .asString()
            .contains("name")
    }

    @Test
    fun `상품명이 255자를 넘으면 400 INVALID_REQUEST로 응답한다`() {
        val result = post("/api/v1/orders", orderJson(productName = "a".repeat(256)))

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        assertThat(result)
            .bodyJson()
            .extractingPath("$.error.message")
            .asString()
            .contains("productName")
    }

    @Test
    fun `금액이 컬럼 자릿수를 넘으면 400 INVALID_REQUEST로 응답한다`() {
        val result = post("/api/v1/orders", orderJson(amount = "99999999999999999999"))

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        assertThat(result)
            .bodyJson()
            .extractingPath("$.error.message")
            .asString()
            .contains("amount")
    }

    @Test
    fun `금액의 소수 자릿수가 둘을 넘으면 반올림해 저장하지 않고 400으로 거절한다`() {
        val result = post("/api/v1/orders", orderJson(amount = "10.999"))

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        assertThat(result)
            .bodyJson()
            .extractingPath("$.error.message")
            .asString()
            .contains("amount")
    }

    @Test
    fun `필수 필드가 빠지면 어느 필드인지 알려 준다`() {
        val result = post("/api/v1/members", """{"email":"missing-name@example.com"}""")

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        assertThat(result).bodyJson().extractingPath("$.error.code").isEqualTo("INVALID_REQUEST")
        assertThat(result)
            .bodyJson()
            .extractingPath("$.error.message")
            .asString()
            .startsWith("name:")
    }

    @Test
    fun `필드 타입이 맞지 않으면 어느 필드인지 알려 주되 파서 내부 메시지는 싣지 않는다`() {
        val result = post("/api/v1/orders", orderJson(amount = "\"not-a-number\""))

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
        assertThat(result)
            .bodyJson()
            .extractingPath("$.error.message")
            .asString()
            .startsWith("amount:")
            .doesNotContain("java.", "tools.jackson", "BigDecimal")
    }

    @Test
    fun `대소문자만 다른 이메일로 다시 가입하면 409 DUPLICATE_EMAIL로 응답한다`() {
        val first = post("/api/v1/members", """{"name":"대문자","email":"Api.Case@Example.com"}""")
        assertThat(first).hasStatus(HttpStatus.CREATED)
        assertThat(first).bodyJson().extractingPath("$.data.email").isEqualTo("api.case@example.com")

        val second = post("/api/v1/members", """{"name":"소문자","email":"api.case@example.com"}""")

        assertThat(second).hasStatus(HttpStatus.CONFLICT)
        assertThat(second).bodyJson().extractingPath("$.error.code").isEqualTo("DUPLICATE_EMAIL")
    }

    @Test
    fun `돌려줄 데이터가 없는 성공 응답은 data가 빈 객체가 아니라 null이다`() {
        val created = post("/api/v1/members", """{"name":"탈퇴예정","email":"no-data@example.com"}""")
        val memberId = Regex(""""id":(\d+)""").find(created.response.contentAsString)!!.groupValues[1]

        val result = mvc.post().uri("/api/v1/members/$memberId/deactivate").exchange()

        assertThat(result).hasStatusOk()
        assertThat(result).bodyJson().extractingPath("$.success").isEqualTo(true)
        assertThat(result).bodyJson().extractingPath("$.data").isNull()
    }

    private fun post(
        uri: String,
        json: String,
    ): MvcTestResult =
        mvc
            .post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .exchange()

    private fun orderJson(
        productName: String = "상품",
        amount: String = "1000",
    ): String = """{"memberId":1,"productName":"$productName","amount":$amount}"""
}
