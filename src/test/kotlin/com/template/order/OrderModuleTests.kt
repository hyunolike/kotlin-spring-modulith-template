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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.modulith.test.Scenario
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal

@ApplicationModuleTest
@Import(TestcontainersConfiguration::class)
class OrderModuleTests(
    @Autowired private val orderService: OrderService,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) {
    @MockitoBean
    private lateinit var memberApi: MemberApi

    @Test
    fun `활성 회원은 주문할 수 있다`() {
        given(memberApi.getMemberWithSharedLock(1L)).willReturn(activeMember(1L))

        val order = orderService.placeOrder(1L, "기계식 키보드", BigDecimal("120000"))

        assertThat(order.id).isPositive()
        assertThat(order.status).isEqualTo(OrderStatus.PLACED)
    }

    @Test
    fun `비활성화된 회원은 주문할 수 없다`() {
        given(memberApi.getMemberWithSharedLock(2L)).willReturn(
            activeMember(2L).copy(status = MemberStatus.DEACTIVATED),
        )

        assertThatThrownBy { orderService.placeOrder(2L, "모니터", BigDecimal("300000")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_DEACTIVATED)
    }

    @Test
    fun `존재하지 않는 회원의 주문은 실패한다`() {
        given(memberApi.getMemberWithSharedLock(99L)).willThrow(BusinessException(ErrorCode.MEMBER_NOT_FOUND))

        assertThatThrownBy { orderService.placeOrder(99L, "마우스", BigDecimal("45000")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
    }

    @Test
    fun `회원 비활성화 이벤트를 받으면 해당 회원의 주문을 모두 취소한다`(scenario: Scenario) {
        given(memberApi.getMemberWithSharedLock(3L)).willReturn(activeMember(3L))
        orderService.placeOrder(3L, "노트북 거치대", BigDecimal("35000"))
        orderService.placeOrder(3L, "USB 허브", BigDecimal("28000"))

        scenario
            .publish(MemberDeactivatedEvent(3L))
            .andWaitForStateChange(
                { orderService.getOrders(3L, Pageable.unpaged()).content },
                { orders -> orders.isNotEmpty() && orders.all { it.status == OrderStatus.CANCELLED } },
            ).andVerify { orders ->
                assertThat(orders).hasSize(2)
            }
    }

    @Test
    fun `주문 목록은 요청한 크기만큼 페이지로 나눠 조회한다`() {
        given(memberApi.getMemberWithSharedLock(4L)).willReturn(activeMember(4L))
        val placed = (1..5).map { orderService.placeOrder(4L, "상품$it", BigDecimal("1000")) }
        val latestFirst = Sort.by(Sort.Direction.DESC, "id")

        val firstPage = orderService.getOrders(4L, PageRequest.of(0, 2, latestFirst))
        val lastPage = orderService.getOrders(4L, PageRequest.of(2, 2, latestFirst))

        assertThat(firstPage.totalElements).isEqualTo(5)
        assertThat(firstPage.totalPages).isEqualTo(3)
        assertThat(firstPage.content.map { it.id }).containsExactly(placed[4].id, placed[3].id)
        assertThat(lastPage.content.map { it.id }).containsExactly(placed[0].id)
        assertThat(lastPage.hasNext()).isFalse()
    }

    @Test
    fun `모듈 테스트는 자기 모듈과 shared 모듈의 마이그레이션만 실행한다`() {
        val tables =
            jdbcTemplate.queryForList(
                "select tablename from pg_tables where schemaname = current_schema()",
                String::class.java,
            )

        assertThat(tables)
            .contains("orders", "flyway_schema_history_order")
            .contains("event_publication", "flyway_schema_history_shared")
            .doesNotContain("members", "flyway_schema_history_member")
    }

    @Test
    fun `회원별 주문 조회용 (member_id, status) 복합 인덱스가 있다`() {
        val indexDefinitions =
            jdbcTemplate.queryForList(
                "select indexdef from pg_indexes where tablename = 'orders'",
                String::class.java,
            )

        assertThat(indexDefinitions).anyMatch { it?.contains("(member_id, status)") == true }
    }

    private fun activeMember(id: Long): MemberInfo =
        MemberInfo(id = id, name = "회원$id", email = "member$id@example.com", status = MemberStatus.ACTIVE)
}
