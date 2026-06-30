package com.example.team3plusspring.domain.order.entity;

import com.example.team3plusspring.global.entity.BaseEntity;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false)
    private int totalProductAmount;

    private int usedCouponAmount;

    @Column(nullable = false)
    private int paymentAmount;

    private LocalDateTime canceled_at;

    private Order(Long userId, int totalProductAmount, int usedCouponAmount) {
        validateOrderAmount(totalProductAmount, usedCouponAmount);

        this.userId = userId;
        this.orderNumber = generateOrderNumber();
        this.status = OrderStatus.READY;
        this.totalProductAmount = totalProductAmount;
        this.usedCouponAmount = usedCouponAmount;
        this.paymentAmount = totalProductAmount - usedCouponAmount;
    }

    public static Order create(Long userId, int totalProductAmount, int usedCouponAmount) {
        return new Order(userId, totalProductAmount, usedCouponAmount);
    }

    public void markAsPaymentPending() {
        changeStatus(OrderStatus.PAYMENT_PENDING);
    }

    public void markAsCompleted() {
        changeStatus(OrderStatus.COMPLETED);
    }

    public void markAsCancelled() {
        changeStatus(OrderStatus.CANCELED);
        this.canceled_at = LocalDateTime.now();
    }

    private String generateOrderNumber() {
        return "order_" + UUID.randomUUID();
    }

    // 주문 상태 변경 검증
    private void changeStatus(OrderStatus newStatus) {
        if (!this.status.canTransitTo(newStatus)) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        this.status = newStatus;
    }

    // 할인금액 검증
    private void validateOrderAmount(int totalProductAmount, int usedCouponAmount) {
        if (totalProductAmount < usedCouponAmount) {
            throw new BusinessException(ErrorCode.ORDER_DISCOUNT_AMOUNT_EXCEEDED);
        }
    }
}
