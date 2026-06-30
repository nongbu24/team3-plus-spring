package com.example.team3plusspring.domain.cart.repository;

import com.example.team3plusspring.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartId(@Param("cartId") Long cartId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.id IN :cartItemIds ORDER BY ci.productId")
    List<CartItem> findByCartIdAndIdInOrderByProductId(@Param("cartItemIds") List<Long> cartItemIds, @Param("cartId") Long cartId);
}
