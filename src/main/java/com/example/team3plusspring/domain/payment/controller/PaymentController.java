package com.example.team3plusspring.domain.payment.controller;

import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentRequest;
import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentResponse;
import com.example.team3plusspring.domain.payment.dto.StartPaymentResponse;
import com.example.team3plusspring.domain.payment.facade.PaymentFacade;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/{paymentId}/start")
    public ResponseEntity<ApiResponse<StartPaymentResponse>> startPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long paymentId
    ) {
        StartPaymentResponse response = paymentFacade.start(userDetails.getUserId(), paymentId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{paymentId}/free-complete")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> completeFreePayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long paymentId
    ) {
        ConfirmPaymentResponse response = paymentFacade.completeFree(userDetails.getUserId(), paymentId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{paymentId}/abort")
    public ResponseEntity<ApiResponse<Void>> abortPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long paymentId
    ) {
        paymentFacade.abort(userDetails.getUserId(), paymentId);

        return ResponseEntity.ok(ApiResponse.success("결제가 중단되었습니다.", (Void) null));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmPaymentResponse>> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid ConfirmPaymentRequest request
    ) {
        ConfirmPaymentResponse response = paymentFacade.confirm(userDetails.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
