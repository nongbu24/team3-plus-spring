package com.example.team3plusspring.domain.payment.port;

public enum PaymentCancellationStatus {
    REQUESTED,
    SUCCEEDED,
    FAILED,
    UNKNOWN;

    public static PaymentCancellationStatus from(String status) {
        try {
            return valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return UNKNOWN;
        }
    }
}
