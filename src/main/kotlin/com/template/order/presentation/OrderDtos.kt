package com.template.order.presentation

import com.template.order.OrderInfo
import com.template.order.OrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class PlaceOrderRequest(
    @field:Positive
    val memberId: Long,
    @field:NotBlank
    val productName: String,
    @field:Positive
    val amount: BigDecimal,
)

data class OrderResponse(
    val id: Long,
    val memberId: Long,
    val productName: String,
    val amount: BigDecimal,
    val status: OrderStatus,
) {
    companion object {
        fun from(info: OrderInfo): OrderResponse =
            OrderResponse(
                id = info.id,
                memberId = info.memberId,
                productName = info.productName,
                amount = info.amount,
                status = info.status,
            )
    }
}
