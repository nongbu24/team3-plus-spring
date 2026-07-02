package com.example.team3plusspring.domain.payment.service;

import com.example.team3plusspring.domain.coupon.service.UserCouponService;
import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderItem;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import com.example.team3plusspring.domain.order.service.OrderService;
import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceTest {

    private static final Long PAYMENT_ID = 10L;
    private static final Long ORDER_ID = 20L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    PaymentService paymentService;

    @Mock
    OrderService orderService;

    @Mock
    ProductService productService;

    @Mock
    UserCouponService userCouponService;

    @InjectMocks
    PaymentCommandService paymentCommandService;

    @Test
    void 결제시작_준비상태주문과대기상태결제이면_주문을결제대기로변경한다() {
        // given
        Payment payment = payment();
        Order order = readyOrder(USER_ID);

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when
        Payment result = paymentCommandService.startPayment(USER_ID, PAYMENT_ID);

        // then
        assertThat(result).isSameAs(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 결제시작_이미시작된결제이면_같은결제를반환한다() {
        // given
        Payment payment = payment();
        Order order = order();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when
        Payment result = paymentCommandService.startPayment(USER_ID, PAYMENT_ID);

        // then
        assertThat(result).isSameAs(payment);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 결제시작_타인의주문이면_실패한다() {
        // given
        Payment payment = payment();
        Order order = readyOrder(OTHER_USER_ID);

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when & then
        assertThatThrownBy(() -> paymentCommandService.startPayment(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 결제완료_결제대기상태이면_결제와주문을완료한다() {
        // given
        Payment payment = payment();
        Order order = order();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when
        Payment result = paymentCommandService.completePayment(PAYMENT_ID);

        // then
        assertThat(result).isSameAs(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getApprovedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 결제완료_이미완료된결제이면_주문을다시처리하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsPaid();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when
        Payment result = paymentCommandService.completePayment(PAYMENT_ID);

        // then
        assertThat(result).isSameAs(payment);
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    @Test
    void 무료결제완료_0원결제이면_결제와주문을완료한다() {
        // given
        Payment payment = zeroPayment();
        Order order = zeroReadyOrder(USER_ID);

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when
        Payment result = paymentCommandService.completeFreePayment(USER_ID, PAYMENT_ID);

        // then
        assertThat(result).isSameAs(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getApprovedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 무료결제완료_이미결제시작된0원결제이면_주문을완료한다() {
        // given
        Payment payment = zeroPayment();
        Order order = zeroReadyOrder(USER_ID);
        order.markAsPaymentPending();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when
        Payment result = paymentCommandService.completeFreePayment(USER_ID, PAYMENT_ID);

        // then
        assertThat(result).isSameAs(payment);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 무료결제완료_0원이아니면_상태를변경하지않고실패한다() {
        // given
        Payment payment = payment();
        Order order = readyOrder(USER_ID);

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);

        // when & then
        assertThatThrownBy(() -> paymentCommandService.completeFreePayment(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FREE_AMOUNT_REQUIRED));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 결제실패_결제대기상태이면_주문을취소하고재고와쿠폰을복구한다() {
        // given
        Payment payment = payment();
        Order order = order();
        List<OrderItem> orderItems = List.of(mock(OrderItem.class));

        givenPaymentOrderAndItems(payment, order, orderItems);

        // when
        paymentCommandService.failPayment(PAYMENT_ID);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(productService).restoreStocks(orderItems);
        verify(userCouponService).restoreCouponByOrderId(ORDER_ID);
    }

    @Test
    void 결제실패_이미실패한결제이면_재고와쿠폰을다시복구하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsFailed();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when
        paymentCommandService.failPayment(PAYMENT_ID);

        // then
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    @Test
    void 결제취소_결제대기상태이면_주문을취소하고재고와쿠폰을복구한다() {
        // given
        Payment payment = payment();
        Order order = order();
        List<OrderItem> orderItems = List.of(mock(OrderItem.class));

        givenPaymentOrderAndItems(payment, order, orderItems);

        // when
        paymentCommandService.cancelPayment(PAYMENT_ID);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(productService).restoreStocks(orderItems);
        verify(userCouponService).restoreCouponByOrderId(ORDER_ID);
    }

    @Test
    void 결제취소_이미취소된결제이면_재고와쿠폰을다시복구하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsCanceled();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when
        paymentCommandService.cancelPayment(PAYMENT_ID);

        // then
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    @Test
    void 결제취소_이미완료된결제이면_상태를변경하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsPaid();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when & then
        assertThatThrownBy(() -> paymentCommandService.cancelPayment(PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderService, never()).findOrderForUpdate(ORDER_ID);
        verifyNoInteractions(productService, userCouponService);
    }

    @Test
    void 결제취소요청_결제대기상태이면_주문을종료하고재고와쿠폰을복구한다() {
        // given
        Payment payment = payment();
        Order order = order();
        List<OrderItem> orderItems = List.of(mock(OrderItem.class));

        givenPaymentOrderAndItems(payment, order, orderItems);

        // when
        boolean started = paymentCommandService.requestCancellation(PAYMENT_ID);

        // then
        assertThat(started).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCEL_REQUESTED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(productService).restoreStocks(orderItems);
        verify(userCouponService).restoreCouponByOrderId(ORDER_ID);
    }

    @Test
    void 결제취소요청_이미요청중이면_주문과재고를다시처리하지않는다() {
        // given
        Payment payment = payment();
        payment.markAsCancelRequested();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when
        boolean started = paymentCommandService.requestCancellation(PAYMENT_ID);

        // then
        assertThat(started).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCEL_REQUESTED);
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    @Test
    void 결제취소완료_취소요청상태이면_복구를반복하지않고결제만취소완료한다() {
        // given
        Payment payment = payment();
        payment.markAsCancelRequested();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when
        paymentCommandService.cancelPayment(PAYMENT_ID);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    @Test
    void 결제수동확인_결제대기상태이면_직접변경하지않는다() {
        // given
        Payment payment = payment();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when & then
        assertThatThrownBy(() -> paymentCommandService.markPaymentForReview(PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    @Test
    void 결제수동확인_취소요청상태이면_복구를반복하지않고상태만변경한다() {
        // given
        Payment payment = payment();
        payment.markAsCancelRequested();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when
        paymentCommandService.markPaymentForReview(PAYMENT_ID);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REVIEW_REQUIRED);
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    @Test
    void 결제취소완료_수동확인상태이면_복구를반복하지않고결제만취소완료한다() {
        // given
        Payment payment = payment();
        payment.markAsCancelRequested();
        payment.markAsReviewRequired();

        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);

        // when
        paymentCommandService.cancelPayment(PAYMENT_ID);

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verifyNoInteractions(orderService, productService, userCouponService);
    }

    private void givenPaymentOrderAndItems(Payment payment, Order order, List<OrderItem> orderItems) {
        when(paymentService.findPaymentForUpdate(PAYMENT_ID)).thenReturn(payment);
        when(orderService.findOrderForUpdate(ORDER_ID)).thenReturn(order);
        when(orderService.findOrderItems(ORDER_ID)).thenReturn(orderItems);
    }

    private Payment payment() {
        Payment payment = Payment.create(ORDER_ID, 10_000, 0);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    private Payment zeroPayment() {
        Payment payment = Payment.create(ORDER_ID, 10_000, 10_000);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    private Order order() {
        Order order = readyOrder(USER_ID);
        order.markAsPaymentPending();

        return order;
    }

    private Order readyOrder(Long userId) {
        Order order = Order.create(userId, 10_000, 0);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        return order;
    }

    private Order zeroReadyOrder(Long userId) {
        Order order = Order.create(userId, 10_000, 10_000);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        return order;
    }
}
