package com.example.team3plusspring.domain.payment.port;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentCancellationResult {
    private final PaymentCancellationStatus status;

    public static PaymentCancellationResult from(String status) {
        return new PaymentCancellationResult(PaymentCancellationStatus.from(status));
    }
}
