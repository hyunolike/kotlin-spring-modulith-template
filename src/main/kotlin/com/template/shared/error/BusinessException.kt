package com.template.shared.error

class BusinessException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message ?: errorCode.message, cause)
