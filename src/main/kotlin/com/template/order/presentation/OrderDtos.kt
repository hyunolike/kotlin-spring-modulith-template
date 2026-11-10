package com.template.order.presentation

import com.template.order.OrderInfo
import com.template.order.OrderStatus
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

// 길이·자릿수 제한은 컬럼 정의(varchar(255), numeric(15, 2))와 맞춘다.
// 자릿수 검증이 없으면 소수 셋째 자리가 저장 시 조용히 반올림되어 응답과 저장 값이 달라진다.
data class PlaceOrderRequest(
    @field:Positive
    val memberId: Long,
    @field:NotBlank
    @field:Size(max = 255)
    val productName: String,
    @field:Positive
    @field:Digits(integer = 13, fraction = 2)
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
