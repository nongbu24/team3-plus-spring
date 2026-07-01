package com.example.team3plusspring.domain.payment.repository;

import com.example.team3plusspring.domain.payment.entity.Payment;
import com.example.team3plusspring.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class PaymentRepositoryTest {

    @Autowired
    PaymentRepository paymentRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void 결제잠금조회_결제아이디가존재하면_결제를반환한다() {
        // given
        Payment savedPayment = paymentRepository.saveAndFlush(Payment.create(1L, 10_000, 0));
        entityManager.clear();

        // when
        Optional<Payment> result = paymentRepository.findByIdForUpdate(savedPayment.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedPayment.getId());
        assertThat(result.get().getOrderId()).isEqualTo(savedPayment.getOrderId());
    }

    @Test
    void 결제잠금조회_주문아이디가존재하면_결제를반환한다() {
        // given
        Payment savedPayment = paymentRepository.saveAndFlush(Payment.create(2L, 20_000, 5_000));
        entityManager.clear();

        // when
        Optional<Payment> result = paymentRepository.findByOrderIdForUpdate(savedPayment.getOrderId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedPayment.getId());
        assertThat(result.get().getOrderId()).isEqualTo(savedPayment.getOrderId());
    }
}
