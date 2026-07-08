package com.template.order.application

import com.template.member.MemberDeactivatedEvent
import com.template.order.OrderStatus
import com.template.order.domain.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class MemberEventListener(
    private val orderRepository: OrderRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * member 모듈의 이벤트를 수신해 해당 회원의 미완료 주문을 모두 취소한다.
     * @ApplicationModuleListener = 트랜잭션 커밋 후 + 비동기 + 새 트랜잭션.
     * Event Publication Registry에 발행 이력이 저장되어 실패 시 재처리할 수 있다.
     */
    @ApplicationModuleListener
    fun on(event: MemberDeactivatedEvent) {
        val cancelled =
            orderRepository
                .findAllByMemberIdAndStatus(event.memberId, OrderStatus.PLACED)
                .onEach { it.cancel() }
        log.info("회원 비활성화로 주문 {}건 취소 (memberId: {})", cancelled.size, event.memberId)
    }
}
