package com.template.member

import com.template.TestcontainersConfiguration
import com.template.member.application.MemberService
import com.template.member.domain.MemberRepository
import com.template.shared.error.BusinessException
import com.template.shared.error.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.modulith.test.Scenario
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ApplicationModuleTest
@Import(TestcontainersConfiguration::class)
class MemberModuleTests(
    @Autowired private val memberService: MemberService,
    @Autowired private val memberRepository: MemberRepository,
    @Autowired private val transactionManager: PlatformTransactionManager,
) {
    @Test
    fun `회원을 등록한다`() {
        val member = memberService.register("홍길동", "hong@example.com")

        assertThat(member.id).isPositive()
        assertThat(member.status).isEqualTo(MemberStatus.ACTIVE)
    }

    @Test
    fun `중복 이메일이면 등록에 실패한다`() {
        memberService.register("김중복", "dup@example.com")

        assertThatThrownBy { memberService.register("이중복", "dup@example.com") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.DUPLICATE_EMAIL)
    }

    @Test
    fun `회원을 비활성화하면 MemberDeactivatedEvent가 발행된다`(scenario: Scenario) {
        val member = memberService.register("박탈퇴", "leave@example.com")

        scenario
            .stimulate { memberService.deactivate(member.id) }
            .andWaitForEventOfType(MemberDeactivatedEvent::class.java)
            .toArriveAndVerify { event ->
                assertThat(event.memberId).isEqualTo(member.id)
            }
    }

    @Test
    fun `이미 비활성화된 회원은 다시 비활성화할 수 없다`() {
        val member = memberService.register("최탈퇴", "left@example.com")
        memberService.deactivate(member.id)

        assertThatThrownBy { memberService.deactivate(member.id) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.MEMBER_ALREADY_DEACTIVATED)
    }

    @Test
    fun `같은 이메일로 동시에 가입하면 하나만 성공하고 나머지는 DUPLICATE_EMAIL로 실패한다`() {
        val attempts = 8
        val ready = CountDownLatch(attempts)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(attempts)

        val futures =
            (1..attempts).map { i ->
                executor.submit<Result<MemberInfo>> {
                    ready.countDown()
                    start.await()
                    runCatching { memberService.register("동시가입$i", "race@example.com") }
                }
            }
        ready.await()
        start.countDown()
        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.mapNotNull { it.exceptionOrNull() })
            .hasSize(attempts - 1)
            .allSatisfy { e ->
                assertThat(e).isInstanceOf(BusinessException::class.java)
                assertThat((e as BusinessException).errorCode).isEqualTo(ErrorCode.DUPLICATE_EMAIL)
            }
    }

    @Test
    fun `먼저 읽은 회원을 다른 트랜잭션이 변경했다면 낙관적 락으로 커밋에 실패한다`() {
        val member = memberService.register("동시탈퇴", "concurrent-leave@example.com")
        val newTransaction =
            TransactionTemplate(transactionManager).apply {
                propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
            }

        assertThatThrownBy {
            newTransaction.executeWithoutResult {
                val stale = memberRepository.findByIdOrNull(member.id)!! // ACTIVE 상태로 읽어 둔다
                newTransaction.executeWithoutResult { memberService.deactivate(member.id) } // 다른 요청이 먼저 커밋
                stale.deactivate() // 읽은 시점 기준으로는 ACTIVE라 검증을 통과한다
            }
        }.isInstanceOf(OptimisticLockingFailureException::class.java)
    }

    @Test
    fun `공유 락 조회는 호출자 트랜잭션 없이 쓸 수 없다`() {
        val member = memberService.register("락회원", "lock@example.com")

        assertThatThrownBy { memberService.getMemberWithSharedLock(member.id) }
            .isInstanceOf(IllegalTransactionStateException::class.java)
    }
}
