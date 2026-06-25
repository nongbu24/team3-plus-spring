package com.example.team3plusspring.domain.order.dto;

import com.example.team3plusspring.domain.order.entity.OrderItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderItemResponse {

    private final Long orderItemId;
    private final Long productId;
    private final String productName;
    private final int quantity;
    private final int unitPrice;
    private final int lineAmount;

    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getLineAmount()
        );
    }
}
