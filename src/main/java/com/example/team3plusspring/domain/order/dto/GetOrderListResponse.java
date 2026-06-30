package com.example.team3plusspring.domain.order.dto;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class GetOrderListResponse {

    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus status;
    private final int totalProductAmount;
    private final int usedCouponAmount;
    private final int paymentAmount;
    private final LocalDateTime createdAt;

    public static GetOrderListResponse from(Order order) {
        return new GetOrderListResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalProductAmount(),
                order.getUsedCouponAmount(),
                order.getPaymentAmount(),
                order.getCreatedAt()
        );
    }
}
