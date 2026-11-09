package com.template.shared.error

import com.template.shared.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * 모든 오류 응답을 ApiResponse 형태로 통일한다.
 *
 * ResponseEntityExceptionHandler를 상속하는 이유: 알 수 없는 경로(404), 지원하지 않는 메서드(405),
 * 파라미터 타입 불일치·누락(400) 같은 Spring MVC 표준 예외를 부모가 먼저 알맞은 상태 코드로 잡아 준다.
 * 상속 없이 @ExceptionHandler(Exception::class)만 두면 이 예외들이 모두 500으로 바뀌고 ERROR 로그가 남는다.
 * 부모가 이미 처리하는 예외(MethodArgumentNotValidException 등)에 @ExceptionHandler를 추가하면
 * 매핑이 중복되어 기동이 실패하므로, 응답 형식은 handleExceptionInternal 하나에서 바꾼다.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("BusinessException: code={}, message={}", e.errorCode.name, e.message)
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode, e.message))
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLockingFailure(e: OptimisticLockingFailureException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("OptimisticLockingFailureException: {}", e.message)
        return ResponseEntity
            .status(ErrorCode.CONCURRENT_MODIFICATION.status)
            .body(ApiResponse.error(ErrorCode.CONCURRENT_MODIFICATION))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Unit>> {
        log.error("Unhandled exception", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR))
    }

    /** 부모가 처리하는 Spring MVC 표준 예외의 응답 본문을 ApiResponse로 바꾼다. 상태 코드는 부모 판단을 그대로 쓴다. */
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val errorCode = errorCodeFor(statusCode)
        val message = (ex as? MethodArgumentNotValidException)?.let(::validationMessage)
        if (statusCode.is5xxServerError) {
            log.error("{}: {}", ex.javaClass.simpleName, ex.message, ex)
        } else {
            log.warn("{}: {}", ex.javaClass.simpleName, ex.message)
        }
        return ResponseEntity
            .status(statusCode)
            .headers(headers)
            .body(ApiResponse.error(errorCode, message))
    }

    private fun errorCodeFor(status: HttpStatusCode): ErrorCode =
        when {
            status == HttpStatus.NOT_FOUND -> ErrorCode.NOT_FOUND
            status == HttpStatus.METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED
            status.is4xxClientError -> ErrorCode.INVALID_REQUEST
            else -> ErrorCode.INTERNAL_ERROR
        }

    private fun validationMessage(e: MethodArgumentNotValidException): String =
        e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
}
