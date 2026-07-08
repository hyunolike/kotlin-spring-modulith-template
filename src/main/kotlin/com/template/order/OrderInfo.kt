package com.template.order

import java.math.BigDecimal

data class OrderInfo(
    val id: Long,
    val memberId: Long,
    val productName: String,
    val amount: BigDecimal,
    val status: OrderStatus,
)
