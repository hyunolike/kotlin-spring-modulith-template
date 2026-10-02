package com.template.member.application

import com.template.member.MemberApi
import com.template.member.MemberDeactivatedEvent
import com.template.member.MemberInfo
import com.template.member.domain.Member
import com.template.member.domain.MemberRepository
import com.template.shared.error.BusinessException
import com.template.shared.error.ErrorCode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : MemberApi {
    @Transactional
    fun register(
        name: String,
        email: String,
    ): MemberInfo {
        if (memberRepository.existsByEmail(email)) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL)
        }
        // 위 사전 검사는 동시 요청 사이의 경쟁을 막지 못한다. 최종 방어선은 email unique 제약이다.
        return try {
            memberRepository.saveAndFlush(Member(name = name, email = email)).toInfo()
        } catch (e: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.DUPLICATE_EMAIL, cause = e)
        }
    }

    override fun getMember(memberId: Long): MemberInfo = findMember(memberId).toInfo()

    @Transactional
    fun deactivate(memberId: Long) {
        val member = findMember(memberId)
        member.deactivate()
        eventPublisher.publishEvent(MemberDeactivatedEvent(member.id))
    }

    private fun findMember(memberId: Long): Member =
        memberRepository.findByIdOrNull(memberId)
            ?: throw BusinessException(ErrorCode.MEMBER_NOT_FOUND)

    private fun Member.toInfo(): MemberInfo = MemberInfo(id = id, name = name, email = email, status = status)
}
