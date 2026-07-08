package com.template.shared.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
    MEMBER_ALREADY_DEACTIVATED(HttpStatus.CONFLICT, "이미 비활성화된 회원입니다."),

    // order
    MEMBER_DEACTIVATED(HttpStatus.CONFLICT, "비활성화된 회원은 주문할 수 없습니다."),
}
