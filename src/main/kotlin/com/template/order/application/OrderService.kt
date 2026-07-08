package com.template.order.application

import com.template.member.MemberApi
import com.template.member.MemberStatus
import com.template.order.OrderInfo
import com.template.order.domain.Order
import com.template.order.domain.OrderRepository
import com.template.shared.error.BusinessException
import com.template.shared.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val memberApi: MemberApi,
) {
    @Transactional
    fun placeOrder(
        memberId: Long,
        productName: String,
        amount: BigDecimal,
    ): OrderInfo {
        val member = memberApi.getMember(memberId)
        if (member.status != MemberStatus.ACTIVE) {
            throw BusinessException(ErrorCode.MEMBER_DEACTIVATED)
        }
        return orderRepository
            .save(Order(memberId = memberId, productName = productName, amount = amount))
            .toInfo()
    }

    fun getOrders(memberId: Long): List<OrderInfo> = orderRepository.findAllByMemberId(memberId).map { it.toInfo() }

    private fun Order.toInfo(): OrderInfo =
        OrderInfo(id = id, memberId = memberId, productName = productName, amount = amount, status = status)
}
