package com.template.shared.error

class BusinessException(
    val errorCode: ErrorCode,
    message: String? = null,
) : RuntimeException(message ?: errorCode.message)
