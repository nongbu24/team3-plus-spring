package com.example.team3plusspring.domain.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class PortOnePaymentResponse {

    private String id;
    private String status;
    private PaymentAmount amount;
}
