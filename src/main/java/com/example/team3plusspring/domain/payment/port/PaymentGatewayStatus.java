package com.example.team3plusspring.domain.payment.port;

public enum PaymentGatewayStatus {
    READY,
    PENDING,
    VIRTUAL_ACCOUNT_ISSUED,
    PAID,
    FAILED,
    CANCELLED,
    PARTIAL_CANCELLED,
    UNKNOWN;

    public static PaymentGatewayStatus from(String status) {
        try {
            return valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return UNKNOWN;
        }
    }
}
