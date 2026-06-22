package com.example.team3plusspring.domain.cart.repository;

import com.example.team3plusspring.domain.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    boolean existsByUserId(Long userId);
}
