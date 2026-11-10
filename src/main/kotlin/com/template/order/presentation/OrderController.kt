package com.template.order.presentation

import com.template.order.application.OrderService
import com.template.shared.request.PageQuery
import com.template.shared.response.ApiResponse
import com.template.shared.response.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
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
        @Valid @ModelAttribute pageQuery: PageQuery,
    ): ApiResponse<PageResponse<OrderResponse>> {
        val orders = orderService.getOrders(memberId, pageQuery.toPageable(LATEST_FIRST))
        return ApiResponse.success(PageResponse.from(orders, OrderResponse::from))
    }

    companion object {
        // ID는 생성 순서대로 증가하므로 최신순 정렬에 쓴다. 값이 겹치지 않아 페이지 경계에서 순서가 흔들리지 않는다.
        private val LATEST_FIRST = Sort.by(Sort.Direction.DESC, "id")
    }
}
