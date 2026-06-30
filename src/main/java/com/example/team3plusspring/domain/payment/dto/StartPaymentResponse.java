package com.example.team3plusspring.domain.payment.dto;

import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StartPaymentResponse {

    private final Long paymentId;
    private final Long orderId;
    private final String portOnePaymentId;
    private final PaymentStatus status;
    private final int paymentAmount;

    public static StartPaymentResponse from(Payment payment) {
        return new StartPaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPortonePaymentId(),
                payment.getStatus(),
                payment.getPaymentAmount()
        );
    }
}
