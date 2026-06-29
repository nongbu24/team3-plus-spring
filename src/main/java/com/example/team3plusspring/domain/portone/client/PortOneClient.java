package com.example.team3plusspring.domain.portone.client;

import com.example.team3plusspring.domain.payment.port.PaymentGateway;
import com.example.team3plusspring.domain.payment.port.PaymentGatewayResponse;
import com.example.team3plusspring.domain.portone.dto.PortOneCancelRequest;
import com.example.team3plusspring.domain.portone.dto.PortOnePaymentResponse;
import com.example.team3plusspring.global.config.PortOneProperties;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortOneClient implements PaymentGateway {

    private final RestClient portOneRestClient;
    private final PortOneProperties portOneProperties;

    @Override
    public PaymentGatewayResponse getPayment(String paymentId) {
        log.info("PortOne 결제 조회: {}", paymentId);

        PortOnePaymentResponse response;

        try {
            response = portOneRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}")
                            .queryParam("storeId", portOneProperties.getStoreId())
                            .build(paymentId))
                    .retrieve()
                    .body(PortOnePaymentResponse.class);
        } catch (RestClientException exception) {
            log.error("PortOne 결제 조회 실패: paymentId={}", paymentId, exception);
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        if (response == null || response.getAmount() == null) {
            log.error("PortOne 결제 조회 응답 누락: paymentId={}", paymentId);
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        return PaymentGatewayResponse.of(
                response.getId(),
                response.getStatus(),
                response.getAmount().getTotal()
        );
    }

    @Override
    public void cancelPayment(String paymentId, String reason) {
        // paymentId logging
        log.info("PortOne 결제 취소 요청: paymentId={}, reason={}", paymentId, reason);

        portOneRestClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .body(PortOneCancelRequest.of(reason, portOneProperties.getStoreId()))
                .retrieve()
                .toBodilessEntity();
    }
}
