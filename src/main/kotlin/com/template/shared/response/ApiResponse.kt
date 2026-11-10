package com.template.shared.response

import com.template.shared.error.ErrorCode

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorResponse?,
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data, error = null)

        /** 돌려줄 데이터가 없는 성공 응답. Unit을 그대로 담으면 data가 빈 객체({})로 직렬화된다. */
        fun success(): ApiResponse<Unit> = ApiResponse(success = true, data = null, error = null)

        fun error(
            errorCode: ErrorCode,
            message: String? = null,
        ): ApiResponse<Unit> =
            ApiResponse(
                success = false,
                data = null,
                error = ErrorResponse(code = errorCode.name, message = message ?: errorCode.message),
            )
    }
}

data class ErrorResponse(
    val code: String,
    val message: String,
)
