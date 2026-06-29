package com.example.team3plusspring.domain.payment.service;

import com.example.team3plusspring.domain.coupon.service.UserCouponService;
import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderItem;
import com.example.team3plusspring.domain.order.service.OrderService;
import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.entity.PaymentStatus;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ProductService productService;
    private final UserCouponService userCouponService;

    @Transactional
    public Payment completePayment(Long paymentId) {
        Payment payment = paymentService.findPaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.PAID) {
            return payment;
        }

        validatePending(payment);

        Order order = orderService.findOrderForUpdate(payment.getOrderId());
        payment.markAsPaid();
        order.markAsCompleted();

        return payment;
    }

    @Transactional
    public void failPayment(Long paymentId) {
        Payment payment = paymentService.findPaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.FAILED) {
            return;
        }

        validatePending(payment);
        payment.markAsFailed();
        cancelOrderAndRestore(payment);
    }

    @Transactional
    public void cancelPayment(Long paymentId) {
        Payment payment = paymentService.findPaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return;
        }

        validatePending(payment);
        payment.markAsCanceled();
        cancelOrderAndRestore(payment);
    }

    private void cancelOrderAndRestore(Payment payment) {
        Order order = orderService.findOrderForUpdate(payment.getOrderId());
        List<OrderItem> orderItems = orderService.findOrderItems(order.getId());

        order.markAsCancelled();
        productService.restoreStocks(orderItems);
        userCouponService.restoreCouponByOrderId(order.getId());
    }

    private void validatePending(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
    }
}
