package com.example.team3plusspring.domain.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.CouponEventStatus;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long>, CouponEventRepositoryCustom {

	boolean existsByNameAndStatus(String name, CouponEventStatus status);
}
