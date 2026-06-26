package com.example.team3plusspring.domain.coupon.dto;

import java.time.LocalDateTime;

import com.example.team3plusspring.domain.coupon.entity.DiscountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class CreateCouponEventRequest {

	@NotBlank(message = "쿠폰 이벤트명 입력은 필수입니다.")
	private String name;

	@NotNull(message = "할인 타입 입력은 필수입니다.")
	private DiscountType discountType;

	@Positive(message = "할인 금액은 0보다 커야 합니다.")
	private int discountAmount;

	@Positive(message = "총 발급 수량은 0보다 커야 합니다.")
	private int totalQuantity;

	@NotNull(message = "발급 시작일시 입력은 필수입니다.")
	private LocalDateTime startsAt;

	@NotNull(message = "발급 종료일시 입력은 필수입니다.")
	private LocalDateTime endsAt;

	@Positive(message = "사용 유효기간은 0보다 커야 합니다.")
	private int validDays;
}
