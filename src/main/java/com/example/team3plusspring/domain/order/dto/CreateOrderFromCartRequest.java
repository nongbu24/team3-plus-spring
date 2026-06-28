package com.example.team3plusspring.domain.order.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class CreateOrderFromCartRequest {

    @NotEmpty(message = "주문할 장바구니 상품을 선택해주세요.")
    private List<Long> cartItemIds;

    private Long userCouponId;
}
