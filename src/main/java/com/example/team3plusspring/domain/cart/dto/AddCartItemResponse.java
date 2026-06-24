package com.example.team3plusspring.domain.cart.dto;

import com.example.team3plusspring.domain.cart.entity.CartItem;
import com.example.team3plusspring.domain.product.entity.Product;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AddCartItemResponse {

    private final Long cartItemId;
    private final Long productId;
    private final String productName;
    private final int quantity;
    private final int unitPrice;
    private final int lineAmount;

    public static AddCartItemResponse of (CartItem cartItem, Product product) {
        return new AddCartItemResponse(
                cartItem.getId(),
                cartItem.getProductId(),
                product.getName(),
                cartItem.getQuantity(),
                product.getPrice(),
                product.getPrice() * cartItem.getQuantity()
        );
    }

}
