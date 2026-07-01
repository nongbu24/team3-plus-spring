package com.example.team3plusspring.domain.coupon.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long>, UserCouponRepositoryCustom{
}
