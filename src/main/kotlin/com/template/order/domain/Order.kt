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
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
@Table(
    name = "orders", // order는 SQL 예약어
    // findAllByMemberId / findAllByMemberIdAndStatus 조회용 (선두 컬럼 member_id 단독 조회도 커버)
    indexes = [Index(name = "idx_orders_member_id_status", columnList = "member_id, status")],
)
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

    @Version // 동시 수정 시 늦게 커밋한 쪽이 OptimisticLockingFailureException으로 실패한다
    var version: Long = 0
        protected set

    fun cancel() {
        status = OrderStatus.CANCELLED
    }
}
