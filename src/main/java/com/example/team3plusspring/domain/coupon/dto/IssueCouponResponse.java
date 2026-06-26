package com.example.team3plusspring.domain.coupon.dto;

import java.time.LocalDateTime;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.entity.UserCouponStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IssueCouponResponse {

	private final Long id;
	private final Long couponEventId;
	private final UserCouponStatus status;
	private final LocalDateTime issuedAt;

	public static IssueCouponResponse from(UserCoupon userCoupon) {
		return new IssueCouponResponse(
			userCoupon.getId(),
			userCoupon.getCouponEventId(),
			userCoupon.getStatus(),
			userCoupon.getIssuedAt()
		);
	}
}
