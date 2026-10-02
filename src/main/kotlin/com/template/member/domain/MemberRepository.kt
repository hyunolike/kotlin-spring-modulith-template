package com.template.member.domain

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface MemberRepository : JpaRepository<Member, Long> {
    fun existsByEmail(email: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_READ) // PostgreSQL: SELECT ... FOR SHARE
    fun findWithSharedLockById(id: Long): Member?
}
