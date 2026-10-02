package com.template.shared.config

import com.template.TestcontainersConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.task.AsyncTaskExecutor
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class MdcPropagationTests(
    @Autowired @Qualifier("applicationTaskExecutor") private val taskExecutor: AsyncTaskExecutor,
) {
    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `비동기 리스너가 쓰는 applicationTaskExecutor에도 requestId가 전파된다`() {
        MDC.put(MdcLoggingFilter.MDC_KEY, "req-async")

        val requestIdInWorker =
            taskExecutor.submit<String?> { MDC.get(MdcLoggingFilter.MDC_KEY) }.get(5, TimeUnit.SECONDS)

        assertThat(requestIdInWorker).isEqualTo("req-async")
    }
}
