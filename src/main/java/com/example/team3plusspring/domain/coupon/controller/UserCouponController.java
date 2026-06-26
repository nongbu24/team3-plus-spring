package com.example.team3plusspring.domain.coupon.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.team3plusspring.domain.coupon.dto.GetUserCouponListResponse;
import com.example.team3plusspring.domain.coupon.service.UserCouponService;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/coupons")
public class UserCouponController {

	private final UserCouponService userCouponService;

	/**
	 * 내가 보유한 쿠폰 중 사용 가능한(발급됨 + 사용기한 안 지남) 쿠폰 목록을 조회하는 API
	 */
	@GetMapping
	public ResponseEntity<ApiResponse<Page<GetUserCouponListResponse>>> getMyCoupons(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) int size) {

		return ResponseEntity.status(HttpStatus.OK)
			.body(ApiResponse.success(userCouponService.getMyCoupons(userDetails.getUserId(), page, size)));
	}
}
