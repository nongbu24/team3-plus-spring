package com.example.team3plusspring.domain.coupon.dto;

import java.time.LocalDateTime;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.CouponEventStatus;
import com.example.team3plusspring.domain.coupon.entity.DiscountType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CouponEventResponse {

	private final Long id;
	private final String name;
	private final DiscountType discountType;
	private final long discountAmount;
	private final int totalQuantity;
	private final int issuedQuantity;
	private final CouponEventStatus status;
	private final LocalDateTime startsAt;
	private final LocalDateTime endsAt;
	private final int validDays;

	public static CouponEventResponse from(CouponEvent couponEvent) {
		return new CouponEventResponse(
			couponEvent.getId(),
			couponEvent.getName(),
			couponEvent.getDiscountType(),
			couponEvent.getDiscountAmount(),
			couponEvent.getTotalQuantity(),
			couponEvent.getIssuedQuantity(),
			couponEvent.getStatus(),
			couponEvent.getStartsAt(),
			couponEvent.getEndsAt(),
			couponEvent.getValidDays()
		);
	}
}
