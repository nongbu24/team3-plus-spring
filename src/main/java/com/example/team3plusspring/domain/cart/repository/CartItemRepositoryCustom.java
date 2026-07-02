package com.example.team3plusspring.domain.cart.repository;

import com.example.team3plusspring.domain.cart.entity.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepositoryCustom {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    List<CartItem> findByCartId(Long cartId);

    List<CartItem> findByCartIdAndIdInOrderByProductId(List<Long> cartItemIds, Long cartId);
}
