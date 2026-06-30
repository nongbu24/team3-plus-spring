package com.example.team3plusspring.domain.coupon.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.team3plusspring.domain.coupon.dto.CouponEventResponse;
import com.example.team3plusspring.domain.coupon.dto.CreateCouponEventRequest;
import com.example.team3plusspring.domain.coupon.dto.GetCouponEventListResponse;
import com.example.team3plusspring.domain.coupon.dto.IssueCouponResponse;
import com.example.team3plusspring.domain.coupon.service.CouponEventService;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupon-events")
@Validated
public class CouponEventController {

	private final CouponEventService couponEventService;

	/**
	 * 쿠폰 이벤트를 등록하는 API
	 * 관리자(ADMIN)만 등록할 수 있음
	 *
	 * @param userDetails 인증된 사용자 정보
	 * @param request 쿠폰 이벤트 등록 요청 DTO
	 * @return 등록된 쿠폰 이벤트 응답 DTO
	 */

	@PostMapping
	public ResponseEntity<ApiResponse<CouponEventResponse>> createCouponEvent(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@Valid @RequestBody CreateCouponEventRequest request) {

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(HttpStatus.CREATED, couponEventService.createCouponEvent(userDetails.getUser().getRole(), request)));
	}

	/**
	 * 발급 중(OPEN)인 쿠폰 이벤트 목록을 조회하는 API
	 * 인증이 필요 없는 공개 API이며, 종료(CLOSED)된 쿠폰 이벤트는 응답에서 제외됨
	 *
	 * @param page 0부터 시작하는 페이지 번호 (기본값 0)
	 * @param size 페이지당 조회할 쿠폰 이벤트 수 (기본값 10)
	 * @return 쿠폰 이벤트 목록 응답 DTO를 담은 페이지
	 */

	@GetMapping
	public ResponseEntity<ApiResponse<Page<GetCouponEventListResponse>>> getCouponEvents(
		@RequestParam(defaultValue = "0") @Min(0) int page,
		@RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(couponEventService.getCouponEvents(page, size)));
	}

	/**
	 * 쿠폰을 발급받는 API
	 * 로그인한 유저가 본인 명의로 특정 쿠폰 이벤트의 쿠폰을 발급받음
	 * 이미 발급받은 적이 있거나, 재고가 소진됐거나, 발급 기간이 아니면 발급할 수 없음
	 *
	 * @param userDetails 인증된 사용자 정보
	 * @param couponEventId 발급받을 쿠폰 이벤트 ID
	 * @return 발급된 쿠폰 응답 DTO
	 */

	@PostMapping("/{couponEventId}/issue")
	public ResponseEntity<ApiResponse<IssueCouponResponse>> issueCoupon(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PathVariable Long couponEventId) {

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.success(HttpStatus.CREATED, couponEventService.issueCoupon(userDetails.getUserId(), couponEventId)));
	}
}
