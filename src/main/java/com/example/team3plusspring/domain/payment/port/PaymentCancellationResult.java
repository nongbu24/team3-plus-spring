package com.example.team3plusspring.domain.payment.port;

public record PaymentCancellationResult(
        PaymentCancellationStatus status
) {
    public static PaymentCancellationResult from(String status) {
        return new PaymentCancellationResult(PaymentCancellationStatus.from(status));
    }
}
