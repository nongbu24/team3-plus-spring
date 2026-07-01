package com.example.team3plusspring.domain.coupon.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CouponEventListCacheResponse {

	private final List<GetCouponEventListResponse> content;
	private final long totalElements;

	public static CouponEventListCacheResponse from(Page<GetCouponEventListResponse> page) {
		return new CouponEventListCacheResponse(
			page.getContent(),
			page.getTotalElements()
		);
	}
}
