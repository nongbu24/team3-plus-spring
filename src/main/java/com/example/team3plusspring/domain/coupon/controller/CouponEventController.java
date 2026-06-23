package com.example.team3plusspring.domain.coupon.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.team3plusspring.domain.coupon.dto.CouponEventResponse;
import com.example.team3plusspring.domain.coupon.dto.CreateCouponEventRequest;
import com.example.team3plusspring.domain.coupon.service.CouponEventService;
import com.example.team3plusspring.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupon-events")
public class CouponEventController {

	private final CouponEventService couponEventService;

	@PostMapping
	public ResponseEntity<ApiResponse<CouponEventResponse>> createCouponEvent(@Valid @RequestBody CreateCouponEventRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(HttpStatus.CREATED, couponEventService.createCouponEvent(request)));
	}
}
