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

    private Order order() {
        Order order = Order.create(1L, 10_000, 0);
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        return order;
    }
}
