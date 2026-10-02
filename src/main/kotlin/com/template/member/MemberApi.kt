package com.template.member

/**
 * 다른 모듈이 member 모듈에 접근할 때 사용하는 파사드.
 * 모듈 루트 패키지의 타입만 외부에 노출된다 (Spring Modulith 기본 규칙).
 */
interface MemberApi {
    fun getMember(memberId: Long): MemberInfo

    /**
     * 회원을 조회하면서 호출자 트랜잭션이 끝날 때까지 공유 락(SELECT ... FOR SHARE)을 건다.
     * 그동안 회원 비활성화는 대기하고, 이미 진행 중인 비활성화가 있으면 그 커밋을 기다린 뒤 최신 상태를 돌려준다.
     * 조회한 상태를 근거로 다른 모듈이 데이터를 쓸 때 사용하며, 쓰기 트랜잭션 안에서만 호출할 수 있다.
     */
    fun getMemberWithSharedLock(memberId: Long): MemberInfo
}
