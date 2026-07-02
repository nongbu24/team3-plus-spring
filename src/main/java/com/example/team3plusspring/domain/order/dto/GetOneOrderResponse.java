package com.example.team3plusspring.domain.order.dto;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class GetOneOrderResponse {

    private final Long orderId;
    private final Long paymentId;
    private final String orderNumber;
    private final OrderStatus status;
    private final int totalProductAmount;
    private final int usedCouponAmount;
    private final int paymentAmount;
    private final List<OrderItemResponse> items;
    private final LocalDateTime createdAt;
    private final LocalDateTime canceledAt;

    public static GetOneOrderResponse of(Order order, Long paymentId, List<OrderItemResponse> items) {
        return new GetOneOrderResponse(
                order.getId(),
                paymentId,
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalProductAmount(),
                order.getUsedCouponAmount(),
                order.getPaymentAmount(),
                items,
                order.getCreatedAt(),
                order.getCanceled_at()
        );
    }
}
