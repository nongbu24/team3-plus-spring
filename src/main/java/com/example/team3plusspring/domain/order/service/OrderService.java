package com.example.team3plusspring.domain.order.service;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderItem;
import com.example.team3plusspring.domain.order.repository.OrderItemRepository;
import com.example.team3plusspring.domain.order.repository.OrderRepository;
import com.example.team3plusspring.domain.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order createOrder(Long userId, int totalProductAmount, int usedCouponAmount) {
        Order order = Order.create(userId, totalProductAmount, usedCouponAmount);
        return orderRepository.save(order);
    }

    @Transactional
    public OrderItem createOrderItem(Order order, Product product, int quantity) {
        OrderItem orderItem = OrderItem.create(
                order,
                product.getId(),
                product.getName(),
                product.getPrice(),
                quantity
        );

        return orderItemRepository.save(orderItem);
    }
}
