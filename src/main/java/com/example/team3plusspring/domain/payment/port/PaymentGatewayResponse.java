package com.example.team3plusspring.domain.payment.port;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentGatewayResponse {

    private final String id;
    private final String status;
    private final int totalAmount;

    // 결제 게이트웨이 응답 정보를 생성한다.
    public static PaymentGatewayResponse of(String id, String status, int totalAmount) {
        return new PaymentGatewayResponse(id, status, totalAmount);
    }
}
