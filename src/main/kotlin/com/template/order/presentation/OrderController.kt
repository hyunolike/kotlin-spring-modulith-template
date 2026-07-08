package com.template.order.presentation

import com.template.order.application.OrderService
import com.template.shared.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun placeOrder(
        @Valid @RequestBody request: PlaceOrderRequest,
    ): ApiResponse<OrderResponse> {
        val order = orderService.placeOrder(request.memberId, request.productName, request.amount)
        return ApiResponse.success(OrderResponse.from(order))
    }

    @GetMapping
    fun getOrders(
        @RequestParam memberId: Long,
    ): ApiResponse<List<OrderResponse>> = ApiResponse.success(orderService.getOrders(memberId).map(OrderResponse::from))
}
