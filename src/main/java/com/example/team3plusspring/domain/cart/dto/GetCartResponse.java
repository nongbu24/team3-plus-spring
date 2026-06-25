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


    public static GetCartResponse of(Long cartId, List<CartItemDetailResponse> items, int totalQuantity, int totalAmount) {
        return new GetCartResponse(cartId, items, totalQuantity, totalAmount);
    }
}