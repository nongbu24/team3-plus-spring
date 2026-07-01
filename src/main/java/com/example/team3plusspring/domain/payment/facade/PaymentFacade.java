package com.example.team3plusspring.domain.payment.facade;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import com.example.team3plusspring.domain.order.service.OrderService;
import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentRequest;
import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentResponse;
import com.example.team3plusspring.domain.payment.dto.StartPaymentResponse;
import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import com.example.team3plusspring.domain.payment.port.PaymentCancellationResult;
import com.example.team3plusspring.domain.payment.port.PaymentCancellationStatus;
import com.example.team3plusspring.domain.payment.port.PaymentGateway;
import com.example.team3plusspring.domain.payment.port.PaymentGatewayResponse;
import com.example.team3plusspring.domain.payment.port.PaymentGatewayStatus;
import com.example.team3plusspring.domain.payment.service.PaymentCommandService;
import com.example.team3plusspring.domain.payment.service.PaymentService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {
    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;

    public StartPaymentResponse start(Long userId, Long paymentId) {
        Payment payment = paymentCommandService.startPayment(userId, paymentId);
        return StartPaymentResponse.from(payment);
    }

    // 클라이언트 결제 완료 콜백 이후 서버에서 결제를 확정한다.
    public ConfirmPaymentResponse confirm(Long userId, @Valid ConfirmPaymentRequest request) {
        // 결제 조회 + 주문 조회
        Payment payment = paymentService.findPayment(request.getPaymentId());
        Order order = orderService.findOrder(payment.getOrderId());

        // 본인 주문/결제인지 소유권 검증
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }

        // 이미 결제 완료된 요청은 중복 확정 요청으로 보고 멱등하게 성공 응답을 반환한다.
        if (payment.getStatus() == PaymentStatus.PAID) {
            return ConfirmPaymentResponse.from(payment);
        }

        // 취소가 이미 접수된 결제는 PortOne 취소 API를 다시 호출하지 않고 기존 처리 결과를 반환한다.
        if (payment.getStatus() == PaymentStatus.CANCEL_REQUESTED) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_PENDING);
        }

        // 취소 결과를 자동으로 확정할 수 없는 결제는 중복 요청을 막고 수동 확인 대상으로 유지한다.
        if (payment.getStatus() == PaymentStatus.REVIEW_REQUIRED) {
            throw new BusinessException(ErrorCode.PAYMENT_REVIEW_REQUIRED);
        }

        if (order.getStatus() == OrderStatus.READY) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_STARTED);
        }

        // 결제 진행 상태가 아니면 결제 확정 처리가 불가능하다.
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        // 클라이언트가 보낸 portOnePaymentId와 서버에 저장된 portOnePaymentId가 일치하는지 검증한다.
        String portonePaymentId = payment.getPortonePaymentId();
        if (!portonePaymentId.equals(request.getPortOnePaymentId())) {
            log.warn("결제 승인 거부 - portonePaymentId 불일치 : DB={}, 요청={}", portonePaymentId, request.getPortOnePaymentId());
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        // PortOne API로 실제 결제 정보를 조회한다. 클라이언트가 보낸 결제 결과는 그대로 신뢰하지 않는다.
        PaymentGatewayResponse pgPayment = paymentGateway.getPayment(portonePaymentId);
        validateGatewayPaymentId(portonePaymentId, pgPayment);

        return confirmByGatewayStatus(payment, pgPayment);
    }

    private void validateGatewayPaymentId(
            String expectedPaymentId,
            PaymentGatewayResponse pgPayment
    ) {
        if (expectedPaymentId.equals(pgPayment.getId())) {
            return;
        }

        log.error("PortOne 결제 조회 응답 ID 불일치: expected={}, actual={}",
                expectedPaymentId, pgPayment.getId());
        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
    }

    private ConfirmPaymentResponse confirmByGatewayStatus(
            Payment payment,
            PaymentGatewayResponse pgPayment
    ) {
        PaymentGatewayStatus status = pgPayment.getStatus();

        return switch (status) {
            case PAID -> confirmPaidPayment(payment, pgPayment);
            case FAILED -> failPayment(payment);
            case CANCELLED -> cancelPayment(payment);
            case READY, PENDING, VIRTUAL_ACCOUNT_ISSUED -> throw new BusinessException(
                    ErrorCode.PAYMENT_NOT_COMPLETED
            );
            case PARTIAL_CANCELLED, UNKNOWN -> {
                log.error("결제 승인 보류 - 처리할 수 없는 PG 상태: paymentId={}, pgStatus={}",
                        payment.getId(), status);
                throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
            }
        };
    }

    private ConfirmPaymentResponse confirmPaidPayment(
            Payment payment,
            PaymentGatewayResponse pgPayment
    ) {
        validatePaymentAmount(payment, pgPayment);

        Payment confirmedPayment = paymentCommandService.completePayment(payment.getId());
        return ConfirmPaymentResponse.from(confirmedPayment);
    }

    private ConfirmPaymentResponse failPayment(Payment payment) {
        paymentCommandService.failPayment(payment.getId());
        throw new BusinessException(ErrorCode.PAYMENT_STATUS_NOT_PAID);
    }

    private ConfirmPaymentResponse cancelPayment(Payment payment) {
        paymentCommandService.cancelPayment(payment.getId());
        throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }

    private void validatePaymentAmount(
            Payment payment,
            PaymentGatewayResponse pgPayment
    ) {
        if (payment.getPaymentAmount() == pgPayment.getTotalAmount()) {
            return;
        }

        log.error("결제 승인 실패 - 금액 불일치: paymentId={}, DB금액={}, PG금액={}",
                payment.getId(), payment.getPaymentAmount(), pgPayment.getTotalAmount());

        boolean cancellationStarted = paymentCommandService.requestCancellation(payment.getId());

        if (!cancellationStarted) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_PENDING);
        }

        PaymentCancellationResult cancellation;

        try {
            cancellation = paymentGateway.cancelPayment(
                    payment.getPortonePaymentId(),
                    "결제 금액 불일치 자동 취소"
            );
        } catch (BusinessException exception) {
            paymentCommandService.markPaymentForReview(payment.getId());
            throw new BusinessException(ErrorCode.PAYMENT_REVIEW_REQUIRED);
        }

        handleAmountMismatchCancellation(payment, cancellation.status());
    }

    private void handleAmountMismatchCancellation(
            Payment payment,
            PaymentCancellationStatus cancellationStatus
    ) {
        switch (cancellationStatus) {
            case SUCCEEDED -> {
                paymentCommandService.cancelPayment(payment.getId());
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }
            case REQUESTED -> throw new BusinessException(ErrorCode.PAYMENT_CANCEL_PENDING);
            case FAILED, UNKNOWN -> {
                paymentCommandService.markPaymentForReview(payment.getId());
                throw new BusinessException(ErrorCode.PAYMENT_REVIEW_REQUIRED);
            }
        }
    }
}
