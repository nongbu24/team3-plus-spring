package com.example.team3plusspring.domain.coupon.dto;

import java.time.LocalDateTime;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.entity.UserCouponStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GetUserCouponListResponse {

	private final Long id;
	private final Long couponEventId;
	private final UserCouponStatus status;
	private final LocalDateTime issuedAt;
	private final LocalDateTime expiredAt;

	public static GetUserCouponListResponse from(UserCoupon userCoupon) {
		return new GetUserCouponListResponse(
			userCoupon.getId(),
			userCoupon.getCouponEventId(),
			userCoupon.getStatus(),
			userCoupon.getIssuedAt(),
			userCoupon.getExpiredAt()
		);
	}
}
