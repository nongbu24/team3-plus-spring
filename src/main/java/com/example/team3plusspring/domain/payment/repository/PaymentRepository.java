package com.example.team3plusspring.domain.payment.repository;

import com.example.team3plusspring.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom {
    Optional<Payment> findByOrderId(Long orderId);
}
