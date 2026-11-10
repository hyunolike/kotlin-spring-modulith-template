package com.template.shared.config

import com.template.TestcontainersConfiguration
import io.micrometer.tracing.Tracer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

/** 요청을 처리하는 시점의 추적 ID를 그대로 돌려주는 테스트 전용 엔드포인트. */
@RestController
class TraceProbeController(
    private val tracer: Tracer,
    @Qualifier("applicationTaskExecutor") private val taskExecutor: AsyncTaskExecutor,
) {
    @GetMapping("/test/trace")
    fun trace(): Map<String, String?> =
        mapOf(
            "traceId" to tracer.currentSpan()?.context()?.traceId(),
            "mdcTraceId" to MDC.get("traceId"),
        )

    /** 요청 처리 중 비동기 작업을 넘겼을 때 작업 스레드에서 보이는 추적 ID. @ApplicationModuleListener와 같은 실행기를 쓴다. */
    @GetMapping("/test/trace/async")
    fun traceInWorker(): Map<String, String?> =
        mapOf(
            "traceId" to
                taskExecutor
                    .submit<String?> { tracer.currentSpan()?.context()?.traceId() }
                    .get(5, TimeUnit.SECONDS),
        )
}

/**
 * Spring Boot는 테스트에서 추적을 기본으로 꺼 두므로 @AutoConfigureTracing으로 켠다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTracing
@Import(TestcontainersConfiguration::class, TraceProbeController::class)
class TracingTests(
    @Autowired private val mvc: MockMvcTester,
) {
    @Test
    fun `요청마다 추적 ID가 만들어지고 로그 MDC에도 같은 값이 실린다`() {
        val result = mvc.get().uri("/test/trace").exchange()

        assertThat(result).hasStatusOk()
        assertThat(result)
            .bodyJson()
            .extractingPath("$.traceId")
            .asString()
            .matches("[0-9a-f]{32}")
        val traceId = Regex(""""traceId":"([0-9a-f]+)"""").find(result.response.contentAsString)!!.groupValues[1]
        assertThat(result).bodyJson().extractingPath("$.mdcTraceId").isEqualTo(traceId)
    }

    @Test
    fun `앞단 서비스가 보낸 traceparent 헤더의 추적 ID를 이어받는다`() {
        val upstreamTraceId = "4bf92f3577b34da6a3ce929d0e0e4736"

        val result =
            mvc
                .get()
                .uri("/test/trace")
                .header("traceparent", "00-$upstreamTraceId-00f067aa0ba902b7-01")
                .exchange()

        assertThat(result).bodyJson().extractingPath("$.traceId").isEqualTo(upstreamTraceId)
    }

    @Test
    fun `요청의 추적 컨텍스트는 비동기 작업 스레드로 이어진다`() {
        val upstreamTraceId = "0af7651916cd43dd8448eb211c80319c"

        val result =
            mvc
                .get()
                .uri("/test/trace/async")
                .header("traceparent", "00-$upstreamTraceId-b7ad6b7169203331-01")
                .exchange()

        assertThat(result).bodyJson().extractingPath("$.traceId").isEqualTo(upstreamTraceId)
    }
}
