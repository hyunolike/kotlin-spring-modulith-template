package com.template

import com.template.member.application.MemberService
import com.template.order.OrderInfo
import com.template.order.OrderStatus
import com.template.order.application.OrderService
import com.template.shared.error.BusinessException
import com.template.shared.error.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * member ↔ order 모듈 경계를 가로지르는 동시성 시나리오를 실제 DB로 검증한다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class OrderMemberConsistencyTests(
    @Autowired private val memberService: MemberService,
    @Autowired private val orderService: OrderService,
    @Autowired private val transactionManager: PlatformTransactionManager,
) {
    @Test
    fun `주문 트랜잭션이 커밋되기 전에 회원이 비활성화되어도 그 주문은 취소된다`() {
        val member = memberService.register("경합회원", "order-race@example.com")
        val orderPlaced = CountDownLatch(1)
        val releaseOrder = CountDownLatch(1)

        // 1) 회원 상태를 확인하고 주문을 저장했지만 아직 커밋하지 않은 주문 트랜잭션
        val ordering =
            CompletableFuture.supplyAsync {
                TransactionTemplate(transactionManager).execute {
                    orderService.placeOrder(member.id, "경합 상품", BigDecimal("10000")).also {
                        orderPlaced.countDown()
                        releaseOrder.await()
                    }
                }!!
            }
        assertThat(orderPlaced.await(10, TimeUnit.SECONDS)).isTrue()

        // 2) 그 사이 들어온 비활성화 요청은 주문 트랜잭션이 끝날 때까지 기다려야 한다
        val deactivating = CompletableFuture.runAsync { memberService.deactivate(member.id) }
        try {
            Thread.sleep(BLOCKING_CHECK_MILLIS)
            assertThat(deactivating).isNotDone()
        } finally {
            releaseOrder.countDown()
        }
        val order: OrderInfo = ordering.get(10, TimeUnit.SECONDS)
        deactivating.get(10, TimeUnit.SECONDS)

        // 3) 비활성화 이벤트 리스너가 커밋된 주문까지 보고 취소한다
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertThat(orderService.getOrders(member.id).single { it.id == order.id }.status)
                .isEqualTo(OrderStatus.CANCELLED)
        }
    }

    @Test
    fun `비활성화가 커밋 대기 중이면 주문은 그 결과를 기다렸다가 거절된다`() {
        val member = memberService.register("경합회원2", "order-race-2@example.com")
        val deactivated = CountDownLatch(1)
        val releaseDeactivation = CountDownLatch(1)
        val newTransaction =
            TransactionTemplate(transactionManager).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }

        // 1) 비활성화 UPDATE까지 실행했지만 아직 커밋하지 않은 트랜잭션
        val deactivating =
            CompletableFuture.runAsync {
                newTransaction.executeWithoutResult { status ->
                    memberService.deactivate(member.id)
                    status.flush()
                    deactivated.countDown()
                    releaseDeactivation.await()
                }
            }
        assertThat(deactivated.await(10, TimeUnit.SECONDS)).isTrue()

        // 2) 그 사이 들어온 주문은 비활성화 결과를 기다린다
        val ordering =
            CompletableFuture.supplyAsync {
                runCatching { orderService.placeOrder(member.id, "경합 상품", BigDecimal("10000")) }
            }
        try {
            Thread.sleep(BLOCKING_CHECK_MILLIS)
            assertThat(ordering).isNotDone()
        } finally {
            releaseDeactivation.countDown()
        }
        deactivating.get(10, TimeUnit.SECONDS)

        // 3) 커밋된 최신 상태(DEACTIVATED)를 보고 주문을 거절한다
        assertThat(ordering.get(10, TimeUnit.SECONDS).exceptionOrNull())
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_DEACTIVATED)
        assertThat(orderService.getOrders(member.id)).isEmpty()
    }

    companion object {
        private const val BLOCKING_CHECK_MILLIS = 500L
    }
}
