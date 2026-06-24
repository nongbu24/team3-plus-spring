package com.example.team3plusspring.domain.cart.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class GetCartResponse {
    private final Long cartId;
    private final List<CartItemDetailResponse> items;
    private final int totalQuantity;
    private final int totalAmount;

    // 팩토리 메서드: 여러 값을 조합하여 객체를 생성할 때 사용
    public static GetCartResponse of(Long cartId, List<CartItemDetailResponse> items, int totalQuantity, int totalAmount) {
        return new GetCartResponse(cartId, items, totalQuantity, totalAmount);
    }
}