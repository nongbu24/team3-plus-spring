package com.example.team3plusspring.domain.cart.dto;

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

    // 팩토리 메서드: 개별 아이템 상세 정보 생성 시 사용
    public static CartItemDetailResponse of(Long cartItemId, Long productId, String productName,
                                            int quantity, int unitPrice, int lineAmount,
                                            int stock, ProductStatus status) {
        return new CartItemDetailResponse(cartItemId, productId, productName, quantity, unitPrice, lineAmount, stock, status);
    }
}