package com.example.team3plusspring.domain.cart.repository;

import static com.example.team3plusspring.domain.cart.entity.QCartItem.cartItem;

import com.example.team3plusspring.domain.cart.entity.CartItem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class CartItemRepositoryCustomImpl implements CartItemRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(cartItem)
                        .where(
                                cartItem.cart.id.eq(cartId),
                                cartItem.productId.eq(productId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<CartItem> findByCartId(Long cartId) {
        return queryFactory
                .selectFrom(cartItem)
                .where(cartItem.cart.id.eq(cartId))
                .fetch();
    }

    @Override
    public List<CartItem> findByCartIdAndIdInOrderByProductId(List<Long> cartItemIds, Long cartId) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(cartItem)
                .where(
                        cartItem.cart.id.eq(cartId),
                        cartItem.id.in(cartItemIds)
                )
                .orderBy(cartItem.productId.asc())
                .fetch();
    }
}
