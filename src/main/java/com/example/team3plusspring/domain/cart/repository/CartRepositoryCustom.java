package com.example.team3plusspring.domain.cart.repository;

import com.example.team3plusspring.domain.cart.entity.Cart;

import java.util.Optional;

public interface CartRepositoryCustom {
    boolean existsByUserId(Long userId);

    Optional<Cart> findByUserId(Long userId);

    Optional<Cart> findByUserIdForUpdate(Long userId);
}
