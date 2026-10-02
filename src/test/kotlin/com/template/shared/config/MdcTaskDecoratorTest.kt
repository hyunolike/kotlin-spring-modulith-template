package com.template.shared.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MdcTaskDecoratorTest {
    private val decorator = MdcTaskDecorator()
    private val worker = Executors.newSingleThreadExecutor()

    @AfterEach
    fun tearDown() {
        MDC.clear()
        worker.shutdownNow()
    }

    @Test
    fun `작업을 제출한 스레드의 MDC를 작업 스레드로 복사한다`() {
        MDC.put(MdcLoggingFilter.MDC_KEY, "req-1")
        val task = decorator.decorate { assertThat(MDC.get(MdcLoggingFilter.MDC_KEY)).isEqualTo("req-1") }
        MDC.clear()

        worker.submit(task).get(5, TimeUnit.SECONDS)
    }

    @Test
    fun `작업이 끝나면 작업 스레드의 MDC를 원래대로 되돌린다`() {
        MDC.put(MdcLoggingFilter.MDC_KEY, "req-2")
        val task = decorator.decorate {}
        MDC.clear()

        worker.submit(task).get(5, TimeUnit.SECONDS)
        val leftover = worker.submit<String?> { MDC.get(MdcLoggingFilter.MDC_KEY) }.get(5, TimeUnit.SECONDS)

        assertThat(leftover).isNull()
    }
}
