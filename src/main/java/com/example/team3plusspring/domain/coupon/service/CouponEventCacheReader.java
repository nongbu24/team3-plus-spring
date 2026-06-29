package com.example.team3plusspring.domain.coupon.service;

import java.time.LocalDateTime;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.example.team3plusspring.domain.coupon.dto.CouponEventListCacheResponse;
import com.example.team3plusspring.domain.coupon.dto.GetCouponEventListResponse;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponEventCacheReader {

	private final CouponEventRepository couponEventRepository;

	@Cacheable(value = "couponEvents", key = "#page + '-' + #size")
	public CouponEventListCacheResponse readOpenCouponEvents(int page, int size) {

		Pageable pageable = PageRequest.of(page, size);

		Page<GetCouponEventListResponse> result = couponEventRepository
			.findOpenCouponEvents(LocalDateTime.now(), pageable)
			.map(GetCouponEventListResponse::from);

		return CouponEventListCacheResponse.from(result);
	}
}
