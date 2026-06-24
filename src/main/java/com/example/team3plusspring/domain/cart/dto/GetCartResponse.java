package com.example.team3plusspring.domain.cart.dto;

import com.example.team3plusspring.domain.product.entity.ProductStatus;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class GetCartResponse {
    private Long cartId;
    private List<CartItemDetail> items;
    private int totalQuantity;
    private int totalAmount;

    @Getter
    @Builder
    public static class CartItemDetail {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private int quantity;
        private int unitPrice;
        private int lineAmount;
        private int stock;
        private ProductStatus status;
    }
}