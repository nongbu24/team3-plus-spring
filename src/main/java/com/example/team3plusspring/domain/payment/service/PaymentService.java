package com.example.team3plusspring.domain.payment.service;

import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(Long orderId, int totalProductAmount, int usedCouponAmount) {
        Payment payment = Payment.create(orderId, totalProductAmount, usedCouponAmount);
        return paymentRepository.save(payment);
    }
}
