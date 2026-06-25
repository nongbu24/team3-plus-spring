package com.example.team3plusspring.domain.order.repository;

import com.example.team3plusspring.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
