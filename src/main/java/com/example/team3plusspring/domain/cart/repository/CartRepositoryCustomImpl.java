package com.example.team3plusspring.domain.cart.repository;

import static com.example.team3plusspring.domain.cart.entity.QCart.cart;

import com.example.team3plusspring.domain.cart.entity.Cart;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class CartRepositoryCustomImpl implements CartRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsByUserId(Long userId) {
        return queryFactory
                .selectOne()
                .from(cart)
                .where(cart.userId.eq(userId))
                .fetchFirst() != null;
    }

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(cart)
                        .where(cart.userId.eq(userId))
                        .fetchOne()
        );
    }

    @Override
    public Optional<Cart> findByUserIdForUpdate(Long userId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(cart)
                        .where(cart.userId.eq(userId))
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .fetchOne()
        );
    }
}
