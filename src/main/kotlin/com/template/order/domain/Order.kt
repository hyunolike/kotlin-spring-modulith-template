package com.template.order.domain

import com.template.order.OrderStatus
import com.template.shared.domain.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "orders") // order는 SQL 예약어
class Order(
    @Column(nullable = false)
    val memberId: Long,
    @Column(nullable = false)
    val productName: String,
    @Column(nullable = false, precision = 15, scale = 2)
    val amount: BigDecimal,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseTimeEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PLACED
        protected set

    fun cancel() {
        status = OrderStatus.CANCELLED
    }
}
