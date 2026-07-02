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
import com.example.team3plusspring.domain.payment.port.PaymentGateway;
import com.example.team3plusspring.domain.payment.port.PaymentGatewayResponse;
import com.example.team3plusspring.domain.payment.service.PaymentCommandService;
import com.example.team3plusspring.domain.payment.service.PaymentService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    private static final Long USER_ID = 1L;
    private static final Long PAYMENT_ID = 10L;
    private static final Long ORDER_ID = 20L;
    private static final int PAYMENT_AMOUNT = 10_000;

    @Mock
    PaymentService paymentService;

    @Mock
    PaymentCommandService paymentCommandService;

    @Mock
    PaymentGateway paymentGateway;

    @Mock
    OrderService orderService;

    @InjectMocks
    PaymentFacade paymentFacade;

    @Test
    void 결제시작_본인결제이면_PortOne결제정보를반환한다() {
        // given
        Payment payment = payment();
        when(paymentCommandService.startPayment(USER_ID, PAYMENT_ID)).thenReturn(payment);

        // when
        StartPaymentResponse response = paymentFacade.start(USER_ID, PAYMENT_ID);

        // then
        assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getPortOnePaymentId()).isEqualTo(payment.getPortonePaymentId());
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentAmount()).isEqualTo(PAYMENT_AMOUNT);
        verify(paymentCommandService).startPayment(USER_ID, PAYMENT_ID);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void 무료결제완료_0원결제이면_PG를조회하지않고결제를완료한다() {
        // given
        Payment payment = zeroPayment();
        payment.markAsPaid();
        when(paymentCommandService.completeFreePayment(USER_ID, PAYMENT_ID)).thenReturn(payment);

        // when
        ConfirmPaymentResponse response = paymentFacade.completeFree(USER_ID, PAYMENT_ID);

        // then
        assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.getPaymentAmount()).isZero();
        verify(paymentCommandService).completeFreePayment(USER_ID, PAYMENT_ID);
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void 결제중단_PG결제가준비상태이면_결제와주문을취소한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "READY", PAYMENT_AMOUNT));

        // when
        paymentFacade.abort(USER_ID, PAYMENT_ID);

        // then
        verify(paymentCommandService).cancelPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).failPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).completePayment(PAYMENT_ID);
    }

    @Test
    void 결제중단_PG결제가실패상태이면_결제와주문을실패처리한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "FAILED", PAYMENT_AMOUNT));

        // when
        paymentFacade.abort(USER_ID, PAYMENT_ID);

        // then
        verify(paymentCommandService).failPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).cancelPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).completePayment(PAYMENT_ID);
    }

    @Test
    void 결제중단_PG결제가완료상태이면_내부상태를변경하지않고실패한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PAID", PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.abort(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED));
        verifyNoInteractions(paymentCommandService);
    }

    @Test
    void 결제중단_PG결제가처리중이면_내부상태를변경하지않는다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PENDING", PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.abort(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_NOT_COMPLETED));
        verifyNoInteractions(paymentCommandService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELLED", "VIRTUAL_ACCOUNT_ISSUED", "PARTIAL_CANCELLED"})
    void 결제중단_지원하지않는PG상태이면_내부상태를변경하지않고실패한다(String pgStatus) {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, pgStatus, PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.abort(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED));
        verifyNoInteractions(paymentCommandService);
    }

    @Test
    void 결제중단_PG상태를해석할수없으면_외부API오류로실패한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "UNKNOWN", PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.abort(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.EXTERNAL_API_FAILED));
        verifyNoInteractions(paymentCommandService);
    }

    @Test
    void 결제중단_타인의결제이면_PG를조회하지않고실패한다() {
        // given
        Payment payment = payment();
        Order order = order(999L);

        givenPaymentAndOrder(payment, order);

        // when & then
        assertThatThrownBy(() -> paymentFacade.abort(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제중단_이미중단된결제이면_중단처리를반복하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsCanceled();
        Order order = order(USER_ID);
        order.markAsCancelled();

        givenPaymentAndOrder(payment, order);

        // when
        paymentFacade.abort(USER_ID, PAYMENT_ID);

        // then
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제확정_주문이준비상태이면_PG를조회하지않고실패한다() {
        // given
        Payment payment = payment();
        Order order = readyOrder(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());
        givenPaymentAndOrder(payment, order);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_STARTED));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제확정_PG결제가완료되고금액이일치하면_결제를완료한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());
        Payment confirmedPayment = payment();
        confirmedPayment.markAsPaid();

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PAID", PAYMENT_AMOUNT));
        when(paymentCommandService.completePayment(PAYMENT_ID)).thenReturn(confirmedPayment);

        // when
        ConfirmPaymentResponse response = paymentFacade.confirm(USER_ID, request);

        // then
        assertThat(response.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.getPaymentAmount()).isEqualTo(PAYMENT_AMOUNT);
        assertThat(response.getApprovedAt()).isNotNull();
        verify(paymentCommandService).completePayment(PAYMENT_ID);
        verify(paymentGateway, never()).cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소");
    }

    @Test
    void 결제확정_이미완료된결제이면_PG를조회하지않고같은결과를반환한다() {
        // given
        Payment payment = payment();
        payment.markAsPaid();
        Order order = order(USER_ID);
        order.markAsCompleted();
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);

        // when
        ConfirmPaymentResponse response = paymentFacade.confirm(USER_ID, request);

        // then
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제확정_이미취소요청중인결제이면_PG취소를다시요청하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsCancelRequested();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CANCEL_PENDING));
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제확정_수동확인이필요한결제이면_PG취소를다시요청하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsCancelRequested();
        payment.markAsReviewRequired();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REVIEW_REQUIRED));
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제확정_주문소유자가아니면_실패한다() {
        // given
        Payment payment = payment();
        Order order = order(999L);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제확정_PortOne결제아이디가다르면_실패한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request("different-portone-payment-id");

        givenPaymentAndOrder(payment, order);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
        verifyNoInteractions(paymentGateway, paymentCommandService);
    }

    @Test
    void 결제확정_PortOne조회응답아이디가다르면_실패한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(PaymentGatewayResponse.of("different-portone-payment-id", "PAID", PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_FAILED));
        verifyNoInteractions(paymentCommandService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"READY", "PENDING", "VIRTUAL_ACCOUNT_ISSUED"})
    void 결제확정_PG결제가아직완료되지않았으면_내부상태를변경하지않는다(String pgStatus) {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, pgStatus, PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_COMPLETED));
        verifyNoInteractions(paymentCommandService);
    }

    @Test
    void 결제확정_PG결제가실패했으면_내부결제를실패처리한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "FAILED", PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_STATUS_NOT_PAID));
        verify(paymentCommandService).failPayment(PAYMENT_ID);
    }

    @Test
    void 결제확정_PG결제가취소됐으면_내부결제를취소처리한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "CANCELLED", PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED));
        verify(paymentCommandService).cancelPayment(PAYMENT_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PARTIAL_CANCELLED", "UNSUPPORTED_STATUS"})
    void 결제확정_처리할수없는PG상태이면_내부상태를변경하지않는다(String pgStatus) {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, pgStatus, PAYMENT_AMOUNT));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_FAILED));
        verifyNoInteractions(paymentCommandService);
    }

    @Test
    void 결제확정_금액불일치결제취소가완료되면_내부결제를취소한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PAID", PAYMENT_AMOUNT + 1_000));
        when(paymentCommandService.requestCancellation(PAYMENT_ID)).thenReturn(true);
        when(paymentGateway.cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소"))
                .thenReturn(PaymentCancellationResult.from("SUCCEEDED"));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH));
        verify(paymentCommandService).requestCancellation(PAYMENT_ID);
        verify(paymentCommandService).cancelPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).failPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).completePayment(PAYMENT_ID);
    }

    @Test
    void 결제확정_금액불일치결제취소가요청중이면_취소요청상태로변경한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PAID", PAYMENT_AMOUNT + 1_000));
        when(paymentCommandService.requestCancellation(PAYMENT_ID)).thenReturn(true);
        when(paymentGateway.cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소"))
                .thenReturn(PaymentCancellationResult.from("REQUESTED"));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CANCEL_PENDING));
        verify(paymentCommandService).requestCancellation(PAYMENT_ID);
        verify(paymentCommandService, never()).cancelPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).completePayment(PAYMENT_ID);
    }

    @Test
    void 결제확정_다른요청이취소를선점했으면_PortOne취소를중복호출하지않는다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PAID", PAYMENT_AMOUNT + 1_000));
        when(paymentCommandService.requestCancellation(PAYMENT_ID)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_CANCEL_PENDING));
        verify(paymentGateway, never()).cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소");
        verify(paymentCommandService, never()).cancelPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).markPaymentForReview(PAYMENT_ID);
    }

    @Test
    void 결제확정_PortOne취소호출결과를확인할수없으면_수동확인상태로변경한다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PAID", PAYMENT_AMOUNT + 1_000));
        when(paymentCommandService.requestCancellation(PAYMENT_ID)).thenReturn(true);
        when(paymentGateway.cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소"))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_FAILED));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REVIEW_REQUIRED));
        verify(paymentCommandService).markPaymentForReview(PAYMENT_ID);
        verify(paymentCommandService, never()).cancelPayment(PAYMENT_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FAILED", "UNSUPPORTED_STATUS"})
    void 결제확정_금액불일치결제취소결과를확정할수없으면_수동확인상태로변경한다(String cancellationStatus) {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenReturn(pgPayment(payment, "PAID", PAYMENT_AMOUNT + 1_000));
        when(paymentCommandService.requestCancellation(PAYMENT_ID)).thenReturn(true);
        when(paymentGateway.cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소"))
                .thenReturn(PaymentCancellationResult.from(cancellationStatus));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REVIEW_REQUIRED));
        verify(paymentCommandService).markPaymentForReview(PAYMENT_ID);
        verify(paymentCommandService, never()).cancelPayment(PAYMENT_ID);
        verify(paymentCommandService, never()).completePayment(PAYMENT_ID);
    }

    @Test
    void 결제확정_PG조회에실패하면_내부상태를변경하지않는다() {
        // given
        Payment payment = payment();
        Order order = order(USER_ID);
        ConfirmPaymentRequest request = request(payment.getPortonePaymentId());

        givenPaymentAndOrder(payment, order);
        when(paymentGateway.getPayment(payment.getPortonePaymentId()))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_FAILED));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirm(USER_ID, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_FAILED));
        verifyNoInteractions(paymentCommandService);
    }

    private void givenPaymentAndOrder(Payment payment, Order order) {
        when(paymentService.findPayment(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrder(ORDER_ID)).thenReturn(order);
    }

    private Payment payment() {
        Payment payment = Payment.create(ORDER_ID, PAYMENT_AMOUNT, 0);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    private Payment zeroPayment() {
        Payment payment = Payment.create(ORDER_ID, PAYMENT_AMOUNT, PAYMENT_AMOUNT);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    private Order order(Long userId) {
        Order order = readyOrder(userId);
        order.markAsPaymentPending();

        return order;
    }

    private Order readyOrder(Long userId) {
        Order order = Order.create(userId, PAYMENT_AMOUNT, 0);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        return order;
    }

    private ConfirmPaymentRequest request(String portonePaymentId) {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        ReflectionTestUtils.setField(request, "paymentId", PAYMENT_ID);
        ReflectionTestUtils.setField(request, "portOnePaymentId", portonePaymentId);
        return request;
    }

    private PaymentGatewayResponse pgPayment(Payment payment, String status, int totalAmount) {
        return PaymentGatewayResponse.of(payment.getPortonePaymentId(), status, totalAmount);
    }
}
