package com.example.team3plusspring.domain.order.dto;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CancelOrderResponse {

    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus previousStatus;
    private final OrderStatus currentStatus;
    private final LocalDateTime canceledAt;

    public static CancelOrderResponse of(Order order, OrderStatus previousStatus) {
        return new CancelOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                previousStatus,
                order.getStatus(),
                order.getCanceled_at()
        );
    }
}
