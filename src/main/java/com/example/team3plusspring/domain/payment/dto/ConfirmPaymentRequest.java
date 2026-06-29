package com.example.team3plusspring.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;


@Getter
public class ConfirmPaymentRequest {

    @NotNull(message = "결제 ID는 필수 입니다.")
    private Long paymentId;

    @NotBlank(message = "portOne 결제 ID는 필수 입니다.")
    private String portOnePaymentId;
}
