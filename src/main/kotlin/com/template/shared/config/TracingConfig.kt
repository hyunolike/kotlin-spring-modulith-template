package com.template.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.core.task.support.ContextPropagatingTaskDecorator

@Configuration
class TracingConfig {
    /**
     * 추적 컨텍스트(traceId, spanId)를 비동기 작업 스레드로 넘긴다.
     * 이 빈이 없으면 @ApplicationModuleListener가 만든 스팬이 요청의 트레이스와 끊겨 따로 기록된다.
     * Spring Boot는 TaskDecorator 빈이 여러 개면 모두 합쳐 applicationTaskExecutor에 적용한다 (MdcTaskDecorator와 함께 동작).
     */
    @Bean
    fun contextPropagatingTaskDecorator(): TaskDecorator = ContextPropagatingTaskDecorator()
}
