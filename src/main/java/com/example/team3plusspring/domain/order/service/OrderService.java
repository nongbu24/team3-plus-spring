package com.example.team3plusspring.domain.order.service;

import com.example.team3plusspring.domain.order.entity.Order;
import com.example.team3plusspring.domain.order.entity.OrderItem;
import com.example.team3plusspring.domain.order.entity.OrderStatus;
import com.example.team3plusspring.domain.order.repository.OrderItemRepository;
import com.example.team3plusspring.domain.order.repository.OrderRepository;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional
    public Order findOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<OrderItem> findOrderItems(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Page<Order> findOrders(Long userId, OrderStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_PAGINATION);
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        if (status == null) {
            return orderRepository.findAllByUserId(userId, pageable);
        }

        return orderRepository.findAllByUserIdAndStatus(userId, status, pageable);
    }
}
