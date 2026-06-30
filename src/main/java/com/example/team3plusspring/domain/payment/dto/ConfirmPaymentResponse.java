package com.example.team3plusspring.domain.payment.dto;

import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ConfirmPaymentResponse {

    private final Long paymentId;
    private final Long orderId;
    private final String portOnePaymentId;
    private final PaymentStatus status;
    private final int totalProductAmount;
    private final int usedCouponAmount;
    private final int paymentAmount;
    private final LocalDateTime approvedAt;

    public static ConfirmPaymentResponse from(Payment payment) {
        return new ConfirmPaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPortonePaymentId(),
                payment.getStatus(),
                payment.getTotalProductAmount(),
                payment.getUsedCouponAmount(),
                payment.getPaymentAmount(),
                payment.getApprovedAt()
        );
    }
}
