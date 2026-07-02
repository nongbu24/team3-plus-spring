package com.example.team3plusspring.domain.cart.repository;

import com.example.team3plusspring.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long>, CartItemRepositoryCustom {
}
