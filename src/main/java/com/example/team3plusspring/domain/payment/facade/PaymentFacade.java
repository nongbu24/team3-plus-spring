package com.example.team3plusspring.domain.payment.facade;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.service.OrderService;
import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentRequest;
import com.example.team3plusspring.domain.payment.dto.ConfirmPaymentResponse;
import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import com.example.team3plusspring.domain.payment.port.PaymentGateway;
import com.example.team3plusspring.domain.payment.port.PaymentGatewayResponse;
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

    // PortOne 결제 완료 상태값. 문자열 비교 시 매직 스트링을 피하기 위해 상수화
    private static final String PG_STATUS_PAID = "PAID";

    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;
    private final PaymentGateway paymentGateway;
    private final OrderService orderService;

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

        // 결제 대기 상태가 아니면 결제 확정 처리가 불가능하다.
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

        // PortOne 결제 상태가 결제 완료 상태인지 검증한다.
        if (!PG_STATUS_PAID.equals(pgPayment.getStatus())) {
            log.error("결제 승인 실패 - PG 상태 비정상 : paymentId={}, pgStatus={}", payment.getId(), pgPayment.getStatus());

            // PG에서 결제가 완료되지 않은 경우 주문/결제를 실패 처리하고 선차감 재고를 복구한다.
            paymentCommandService.failPayment(payment.getId());
            throw new BusinessException(ErrorCode.PAYMENT_STATUS_NOT_PAID);
        }

        // 서버가 산정한 PG 실결제 금액과 PortOne 승인 금액이 정확히 일치하는지 검증한다.
        if (payment.getPaymentAmount() != pgPayment.getTotalAmount()) {
            log.error("결제 승인 실패 - 금액 불일치 : paymentId={}, DB금액={}, PG금액={}",
                    payment.getId(), payment.getPaymentAmount(), pgPayment.getTotalAmount());

            // 외부 결제는 성공했지만 내부 검증이 실패한 경우 PortOne 결제를 보상 취소한다.
            try {
                paymentGateway.cancelPayment(portonePaymentId, "결제 금액 불일치 자동 취소");
            } catch (Exception e) {
                log.error("PG 자동 취소 실패 : 수동 처리 필요 : portonePaymentId={}", portonePaymentId, e);
            }

            // 내부 주문/결제는 실패 처리하고 선차감 재고를 복구한다.
            paymentCommandService.failPayment(payment.getId());
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 모든 검증이 통과되면 공통 결제 완료 트랜잭션을 호출한다.
        Payment confirmedPayment = paymentCommandService.completePayment(payment.getId());
        return ConfirmPaymentResponse.from(confirmedPayment);
    }
}
