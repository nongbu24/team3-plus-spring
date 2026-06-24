package com.example.team3plusspring.domain.coupon.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.team3plusspring.domain.coupon.dto.CouponEventResponse;
import com.example.team3plusspring.domain.coupon.dto.CreateCouponEventRequest;
import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.CouponEventStatus;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponEventService {

	private final CouponEventRepository couponEventRepository;

	@Transactional
	public CouponEventResponse createCouponEvent(UserRole role, CreateCouponEventRequest request) {

		if (role != UserRole.ADMIN) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		if (couponEventRepository.existsByNameAndStatus(request.getName(), CouponEventStatus.OPEN)) {
			throw new BusinessException(ErrorCode.COUPON_EVENT_NAME_ALREADY_EXISTS);
		}

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
