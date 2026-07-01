package com.example.team3plusspring.domain.order.repository;

import com.example.team3plusspring.domain.order.entity.Order;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static com.example.team3plusspring.domain.order.entity.QOrder.order;

@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom{

    private final JPAQueryFactory queryFactory;


    @Override
    public Optional<Order> findByIdForUpdate(Long orderId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(order)
                        .where(order.id.eq(orderId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
