package com.example.team3plusspring.domain.coupon.repository;

import java.util.Optional;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;

public interface CouponEventRepositoryCustom {

	Optional<CouponEvent> findByIdForUpdate(Long id);
}
