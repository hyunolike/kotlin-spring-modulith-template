package com.template.member.domain

import com.template.member.MemberStatus
import com.template.shared.domain.BaseTimeEntity
import com.template.shared.error.BusinessException
import com.template.shared.error.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "members")
class Member(
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false, unique = true)
    val email: String,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseTimeEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE
        protected set

    @Version // 동시 수정 시 늦게 커밋한 쪽이 OptimisticLockingFailureException으로 실패한다
    var version: Long = 0
        protected set

    fun deactivate() {
        if (status == MemberStatus.DEACTIVATED) {
            throw BusinessException(ErrorCode.MEMBER_ALREADY_DEACTIVATED)
        }
        status = MemberStatus.DEACTIVATED
    }
}
