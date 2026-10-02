package com.template.shared.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `BusinessException을 ErrorCode에 정의된 HTTP 상태로 변환한다`() {
        val response = handler.handleBusinessException(BusinessException(ErrorCode.MEMBER_NOT_FOUND))

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body!!.success).isFalse()
        assertThat(response.body!!.error!!.code).isEqualTo("MEMBER_NOT_FOUND")
    }

    @Test
    fun `낙관적 락 충돌은 409와 CONCURRENT_MODIFICATION 코드로 변환한다`() {
        val response = handler.handleOptimisticLockingFailure(OptimisticLockingFailureException("stale"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body!!.error!!.code).isEqualTo("CONCURRENT_MODIFICATION")
    }

    @Test
    fun `알 수 없는 예외는 500과 INTERNAL_ERROR 코드로 변환한다`() {
        val response = handler.handleException(IllegalStateException("boom"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body!!.error!!.code).isEqualTo("INTERNAL_ERROR")
    }
}
