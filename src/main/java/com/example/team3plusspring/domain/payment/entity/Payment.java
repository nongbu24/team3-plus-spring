package com.example.team3plusspring.domain.payment.entity;

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
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, unique = true, length = 100)
    private String portonePaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(nullable = false)
    private int totalProductAmount;

    private int usedCouponAmount;

    @Column(nullable = false)
    private int paymentAmount;

    private LocalDateTime approvedAt;

    private Payment(Long orderId, int totalProductAmount, int usedCouponAmount) {
        this.orderId = orderId;
        this.portonePaymentId = generatePortOnePaymentId();
        this.status = PaymentStatus.PENDING;
        this.totalProductAmount = totalProductAmount;
        this.usedCouponAmount = usedCouponAmount;
        this.paymentAmount = totalProductAmount - usedCouponAmount;
    }

    public static Payment create(Long orderId, int totalProductAmount, int usedCouponAmount) {
        return new Payment(orderId, totalProductAmount, usedCouponAmount);
    }

    public void markAsPaid() {
        changeStatus(PaymentStatus.PAID);
        this.approvedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        changeStatus(PaymentStatus.FAILED);
    }

    public void markAsCanceled() {
        changeStatus(PaymentStatus.CANCELED);
    }

    public void markAsRefund() {
        changeStatus(PaymentStatus.REFUNDED);
    }

    private String generatePortOnePaymentId() {
        return "pay_" + UUID.randomUUID();
    }

    // 결제 상태 변경 로직
    private void changeStatus(PaymentStatus newStatus) {
        if (!this.status.canTransitTo(newStatus)) {
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_INVALID);
        }
        this.status = newStatus;
    }
}
