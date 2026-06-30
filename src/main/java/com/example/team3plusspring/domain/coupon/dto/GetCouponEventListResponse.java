package com.example.team3plusspring.domain.coupon.dto;

import java.time.LocalDateTime;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.CouponEventStatus;
import com.example.team3plusspring.domain.coupon.entity.DiscountType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GetCouponEventListResponse {

	private final Long id;
	private final String name;
	private final DiscountType discountType;
	private final int discountAmount;
	private final int totalQuantity;
	private final int issuedQuantity;
	private final CouponEventStatus status;
	private final LocalDateTime startsAt;
	private final LocalDateTime endsAt;

	public static GetCouponEventListResponse from(CouponEvent couponEvent) {
		return new GetCouponEventListResponse(
			couponEvent.getId(),
			couponEvent.getName(),
			couponEvent.getDiscountType(),
			couponEvent.getDiscountAmount(),
			couponEvent.getTotalQuantity(),
			couponEvent.getIssuedQuantity(),
			couponEvent.getStatus(),
			couponEvent.getStartsAt(),
			couponEvent.getEndsAt()
		);
	}
}
