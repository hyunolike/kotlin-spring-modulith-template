package com.template.member

import com.template.TestcontainersConfiguration
import com.template.member.application.MemberService
import com.template.shared.error.BusinessException
import com.template.shared.error.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.modulith.test.Scenario

@ApplicationModuleTest
@Import(TestcontainersConfiguration::class)
class MemberModuleTests(
    @Autowired private val memberService: MemberService,
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
}
