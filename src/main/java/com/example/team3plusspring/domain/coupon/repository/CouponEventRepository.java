package com.example.team3plusspring.domain.coupon.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.CouponEventStatus;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {

	boolean existsByNameAndStatus(String name, CouponEventStatus status);

	Page<CouponEvent> findByStatus(CouponEventStatus status, Pageable pageable);
}
