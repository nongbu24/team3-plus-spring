package com.example.team3plusspring.domain.order.repository;

import com.example.team3plusspring.domain.order.entity.Order;

import java.util.Optional;

public interface OrderRepositoryCustom {

    Optional<Order> findByIdForUpdate(Long orderId);
}
