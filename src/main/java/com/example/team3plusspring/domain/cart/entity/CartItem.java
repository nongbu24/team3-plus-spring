package com.example.team3plusspring.domain.cart.entity;

import com.example.team3plusspring.global.entity.BaseEntity;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id", "product_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    private CartItem(Cart cart, Long productId, int quantity) {
        validateQuantity(quantity);

        this.cart = cart;
        this.productId = productId;
        this.quantity = quantity;
    }

    public static CartItem create(Cart cart, Long productId, int quantity) {
        return new CartItem(cart, productId, quantity);
    }

    public void addQuantity(int requestQuantity) {
        validateQuantity(requestQuantity);
        this.quantity += requestQuantity;
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.CART_ITEM_QUANTITY_INVALID);
        }
    }
}
