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
    public Payment startPayment(Long userId, Long paymentId) {
        Payment payment = paymentService.findPaymentForUpdate(paymentId);
        Order order = orderService.findOrderForUpdate(payment.getOrderId());

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }

        if (payment.getStatus() == PaymentStatus.PENDING
                && order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.PENDING
                || order.getStatus() != OrderStatus.READY) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        order.markAsPaymentPending();
        return payment;
    }

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
    public Payment completeFreePayment(Long userId, Long paymentId) {
        Payment payment = paymentService.findPaymentForUpdate(paymentId);
        Order order = orderService.findOrderForUpdate(payment.getOrderId());

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED);
        }

        if (payment.getPaymentAmount() != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_FREE_AMOUNT_REQUIRED);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return payment;
        }

        if (payment.getStatus() != PaymentStatus.PENDING
                || (order.getStatus() != OrderStatus.READY && order.getStatus() != OrderStatus.PAYMENT_PENDING)) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        if (order.getStatus() == OrderStatus.READY) {
            order.markAsPaymentPending();
        }
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

        if (payment.getStatus() == PaymentStatus.CANCEL_REQUESTED
                || payment.getStatus() == PaymentStatus.REVIEW_REQUIRED) {
            payment.markAsCanceled();
            return;
        }

        validatePending(payment);
        payment.markAsCanceled();
        cancelOrderAndRestore(payment);
    }

    @Transactional
    public boolean requestCancellation(Long paymentId) {
        Payment payment = paymentService.findPaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.CANCEL_REQUESTED) {
            return false;
        }

        validatePending(payment);
        payment.markAsCancelRequested();
        cancelOrderAndRestore(payment);
        return true;
    }

    @Transactional
    public void markPaymentForReview(Long paymentId) {
        Payment payment = paymentService.findPaymentForUpdate(paymentId);

        if (payment.getStatus() == PaymentStatus.REVIEW_REQUIRED) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.CANCEL_REQUESTED) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        payment.markAsReviewRequired();
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
