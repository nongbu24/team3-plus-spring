package com.example.team3plusspring.domain.order.dto;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import com.example.team3plusspring.domain.payment.entity.Payment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateOrderResponse {

    private final Long orderId;
    private final Long paymentId;
    private final String orderNumber;
    private final String portOnePaymentId;
    private final OrderStatus status;
    private final int totalProductAmount;
    private final int usedCouponAmount;
    private final int paymentAmount;
    private final List<OrderItemResponse> items;
    private final LocalDateTime createdAt;

    public static CreateOrderResponse of(Order order, List<OrderItemResponse> orderItemResponses, Payment payment) {
        return new CreateOrderResponse(
                order.getId(),
                payment.getId(),
                order.getOrderNumber(),
                payment.getPortonePaymentId(),
                order.getStatus(),
                order.getTotalProductAmount(),
                order.getUsedCouponAmount(),
                order.getPaymentAmount(),
                orderItemResponses,
                order.getCreatedAt()
        );
    }
}
