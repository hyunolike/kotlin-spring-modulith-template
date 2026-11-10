package com.template.order.domain

import com.template.order.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {
    fun findAllByMemberId(
        memberId: Long,
        pageable: Pageable,
    ): Page<Order>

    fun findAllByMemberIdAndStatus(
        memberId: Long,
        status: OrderStatus,
    ): List<Order>
}
