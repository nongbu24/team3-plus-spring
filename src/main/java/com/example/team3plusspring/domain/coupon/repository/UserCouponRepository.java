package com.example.team3plusspring.domain.coupon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

	/**
	 * 같은 회원이 같은 쿠폰 이벤트를 중복으로 발급받았는지 확인한다.
	 * 발급 API에서 쿠폰을 발급해주기 전에 먼저 이 메서드로 체크해서,
	 * 이미 발급받은 적이 있으면 COUPON_ALREADY_ISSUED 예외를 던지는 데 사용한다.
	 */
	boolean existByUserIdAndCouponEventId(Long userId, Long couponEventId);

	/**
	 * 특정 회원이 발급 받은 모든 쿠폰을 조회한다.
	 * "내 쿠폰 목록 조회" API(GET /api/users/me/coupons)에서
	 * 로그인한 유저의 userId로 그 사람이 가진 쿠폰을 전부 가져오는 데 사용한다.
	 */
	List<UserCoupon> findAllByUserId(Long userId);
}
