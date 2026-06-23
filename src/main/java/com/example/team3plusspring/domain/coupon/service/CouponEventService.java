package com.example.team3plusspring.domain.coupon.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team3plusspring.domain.coupon.dto.CouponEventResponse;
import com.example.team3plusspring.domain.coupon.dto.CreateCouponEventRequest;
import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponEventService {

	private final CouponEventRepository couponEventRepository;

	@Transactional
	public CouponEventResponse createCouponEvent(CreateCouponEventRequest request) {

		CouponEvent couponEvent = CouponEvent.create(
			request.getName(),
			request.getDiscountType(),
			request.getDiscountAmount(),
			request.getTotalQuantity(),
			request.getStartsAt(),
			request.getEndsAt()
		);

		CouponEvent savedCouponEvent = couponEventRepository.save(couponEvent);

		return CouponEventResponse.from(savedCouponEvent);
	}
}
