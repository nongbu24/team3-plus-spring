package com.example.team3plusspring.domain.payment.repository;

import com.example.team3plusspring.domain.payment.entity.Payment;

import java.util.Optional;

public interface PaymentRepositoryCustom {

    Optional<Payment> findByIdForUpdate(Long paymentId);

    Optional<Payment> findByOrderIdForUpdate(Long orderId);


}
