package com.example.team3plusspring.domain.payment.controller;

import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentRequest;
import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentResponse;
import com.example.team3plusspring.domain.payment.facade.PaymentFacade;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ConfirmPaymentRequest request
    ) {
        ConfirmPaymentResponse response = paymentFacade.confirm(userDetails.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
