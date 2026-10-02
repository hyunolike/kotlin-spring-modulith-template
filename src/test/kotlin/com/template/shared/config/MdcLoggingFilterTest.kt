package com.template.shared.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class MdcLoggingFilterTest {
    private val filter = MdcLoggingFilter()

    @Test
    fun `유효한 X-Request-Id는 그대로 사용한다`() {
        val response = filterWithRequestId("order-api_42.retry-1")

        assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).isEqualTo("order-api_42.retry-1")
    }

    @Test
    fun `개행이 섞인 X-Request-Id는 버리고 새로 발급한다`() {
        val response = filterWithRequestId("abc\r\nforged-log-line")

        assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).matches("[0-9a-f-]{8}")
    }

    @Test
    fun `너무 긴 X-Request-Id는 버리고 새로 발급한다`() {
        val response = filterWithRequestId("a".repeat(65))

        assertThat(response.getHeader(MdcLoggingFilter.REQUEST_ID_HEADER)).hasSize(8)
    }

    private fun filterWithRequestId(requestId: String): MockHttpServletResponse {
        val request = MockHttpServletRequest().apply { addHeader(MdcLoggingFilter.REQUEST_ID_HEADER, requestId) }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response
    }
}
