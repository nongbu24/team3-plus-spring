package com.example.team3plusspring.domain.cart.dto;

import com.example.team3plusspring.domain.cart.entity.CartItem;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CartItemDetailResponse {
    private final Long cartItemId;
    private final Long productId;
    private final String productName;
    private final int quantity;
    private final int unitPrice;
    private final int lineAmount;
    private final int stock;
    private final ProductStatus status;

    public static CartItemDetailResponse of(CartItem cartItem, Product product) {
        int lineAmount = product.getPrice() * cartItem.getQuantity();
        return new CartItemDetailResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                cartItem.getQuantity(),
                product.getPrice(),
                lineAmount,
                product.getStock(),
                product.getStatus()
        );
    }
}