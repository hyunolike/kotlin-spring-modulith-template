package com.template.order

import com.template.TestcontainersConfiguration
import com.template.member.MemberApi
import com.template.member.MemberDeactivatedEvent
import com.template.member.MemberInfo
import com.template.member.MemberStatus
import com.template.order.application.OrderService
import com.template.shared.error.BusinessException
import com.template.shared.error.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.modulith.test.Scenario
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal

@ApplicationModuleTest
@Import(TestcontainersConfiguration::class)
class OrderModuleTests(
    @Autowired private val orderService: OrderService,
) {
    @MockitoBean
    private lateinit var memberApi: MemberApi

    @Test
    fun `활성 회원은 주문할 수 있다`() {
        given(memberApi.getMember(1L)).willReturn(activeMember(1L))

        val order = orderService.placeOrder(1L, "기계식 키보드", BigDecimal("120000"))

        assertThat(order.id).isPositive()
        assertThat(order.status).isEqualTo(OrderStatus.PLACED)
    }

    @Test
    fun `비활성화된 회원은 주문할 수 없다`() {
        given(memberApi.getMember(2L)).willReturn(
            activeMember(2L).copy(status = MemberStatus.DEACTIVATED),
        )

        assertThatThrownBy { orderService.placeOrder(2L, "모니터", BigDecimal("300000")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_DEACTIVATED)
    }

    @Test
    fun `존재하지 않는 회원의 주문은 실패한다`() {
        given(memberApi.getMember(99L)).willThrow(BusinessException(ErrorCode.MEMBER_NOT_FOUND))

        assertThatThrownBy { orderService.placeOrder(99L, "마우스", BigDecimal("45000")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
    }

    @Test
    fun `회원 비활성화 이벤트를 받으면 해당 회원의 주문을 모두 취소한다`(scenario: Scenario) {
        given(memberApi.getMember(3L)).willReturn(activeMember(3L))
        orderService.placeOrder(3L, "노트북 거치대", BigDecimal("35000"))
        orderService.placeOrder(3L, "USB 허브", BigDecimal("28000"))

        scenario
            .publish(MemberDeactivatedEvent(3L))
            .andWaitForStateChange(
                { orderService.getOrders(3L) },
                { orders -> orders.isNotEmpty() && orders.all { it.status == OrderStatus.CANCELLED } },
            ).andVerify { orders ->
                assertThat(orders).hasSize(2)
            }
    }

    private fun activeMember(id: Long): MemberInfo =
        MemberInfo(id = id, name = "회원$id", email = "member$id@example.com", status = MemberStatus.ACTIVE)
}
