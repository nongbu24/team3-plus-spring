package com.example.team3plusspring.domain.coupon.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;

public interface UserCouponRepositoryCustom {

	/**
	 * 특적 회원이 보유한 쿠폰 중, 아직 사용 가능한 (ISSUED 상태이면서 사용기한이 지나지 않은) 쿠폰만 조회한다.
	 */
	Page<UserCoupon> findUsableUserCoupons(Long userId, LocalDateTime now, Pageable pageable);

	boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId);

	Optional<UserCoupon> findByIdAndUserIdForUpdate(Long userCouponId, Long userId);

	long countByCouponEventId(Long couponEventId);

	Optional<UserCoupon> findByOrderIdForUpdate(Long orderId);
}
