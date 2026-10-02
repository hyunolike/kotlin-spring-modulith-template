package com.template.shared.config

import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator
import org.springframework.stereotype.Component

/**
 * 요청 스레드의 MDC(requestId 등)를 비동기 작업 스레드로 복사한다.
 * Spring Boot가 TaskDecorator 빈을 applicationTaskExecutor에 자동 적용하므로
 * @ApplicationModuleListener(@Async) 로그에서도 같은 requestId로 요청을 추적할 수 있다.
 */
@Component
class MdcTaskDecorator : TaskDecorator {
    override fun decorate(runnable: Runnable): Runnable {
        val callerContext = MDC.getCopyOfContextMap()
        return Runnable {
            val workerContext = MDC.getCopyOfContextMap()
            replaceContext(callerContext)
            try {
                runnable.run()
            } finally {
                replaceContext(workerContext) // 풀 스레드 재사용 시 이전 요청 값이 남지 않게 복원
            }
        }
    }

    private fun replaceContext(context: Map<String, String>?) {
        if (context == null) MDC.clear() else MDC.setContextMap(context)
    }
}
