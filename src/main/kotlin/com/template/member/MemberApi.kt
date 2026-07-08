package com.template.member

/**
 * 다른 모듈이 member 모듈에 접근할 때 사용하는 파사드.
 * 모듈 루트 패키지의 타입만 외부에 노출된다 (Spring Modulith 기본 규칙).
 */
interface MemberApi {
    fun getMember(memberId: Long): MemberInfo
}
