package com.example.team3plusspring.domain.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {

	boolean existsByName(String name);
}
