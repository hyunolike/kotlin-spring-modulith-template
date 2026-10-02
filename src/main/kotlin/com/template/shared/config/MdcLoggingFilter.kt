package com.template.shared.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
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
                ?: UUID.randomUUID().toString().substring(0, 8)
        MDC.put(MDC_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val MDC_KEY = "requestId"
        private val VALID_REQUEST_ID = Regex("^[A-Za-z0-9._-]{1,64}$")
    }
}
