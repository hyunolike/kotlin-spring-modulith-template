package com.template.shared.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

// 순서를 지정하지 않은 필터 빈은 가장 뒤에 실행된다. 맨 앞에 두어야 앞단 필터(예: Spring Security)의
// 로그에도 requestId가 찍힌다.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcLoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // 외부 입력을 그대로 로그·응답 헤더에 쓰지 않는다 (로그 인젝션, 과도한 길이 방지)
        val requestId =
            request
                .getHeader(REQUEST_ID_HEADER)
                ?.takeIf { VALID_REQUEST_ID.matches(it) }
                ?: newRequestId()
        MDC.put(MDC_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    // 8자리(32비트)는 요청이 수만 건만 쌓여도 겹친다. 16자리(64비트)면 로그 검색 구간 안에서 사실상 유일하다.
    private fun newRequestId(): String =
        UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(GENERATED_ID_LENGTH)

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val MDC_KEY = "requestId"
        const val GENERATED_ID_LENGTH = 16
        private val VALID_REQUEST_ID = Regex("^[A-Za-z0-9._-]{1,64}$")
    }
}
