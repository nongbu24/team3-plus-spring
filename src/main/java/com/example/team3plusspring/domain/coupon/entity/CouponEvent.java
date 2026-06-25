package com.example.team3plusspring.domain.coupon.entity;

import java.time.LocalDateTime;

import com.example.team3plusspring.global.entity.BaseEntity;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "coupon_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEvent extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "discount_type", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private DiscountType discountType;

	@Column(name = "discount_amount", nullable = false)
	private int discountAmount;

	@Column(name = "total_quantity", nullable = false)
	private int totalQuantity;

	@Column(name = "issued_quantity", nullable = false)
	private int issuedQuantity;

	@Column(nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private CouponEventStatus status;

	@Column(name = "starts_at", nullable = false)
	private LocalDateTime startsAt;

	@Column(name = "ends_at", nullable = false)
	private LocalDateTime endsAt;

	private CouponEvent(String name, DiscountType discountType, int discountAmount, int totalQuantity, LocalDateTime startsAt, LocalDateTime endsAt) {
		if (discountType == DiscountType.PERCENT && discountAmount > 100) {
			throw new BusinessException(ErrorCode.INVALID_DISCOUNT_AMOUNT);
		}
		if (startsAt.isAfter(endsAt)) {
			throw new BusinessException(ErrorCode.INVALID_COUPON_EVENT_PERIOD);
		}

		this.name = name;
		this.discountType = discountType;
		this.discountAmount = discountAmount;
		this.totalQuantity = totalQuantity;
		this.issuedQuantity = 0;
		this.status = CouponEventStatus.OPEN;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	public static CouponEvent create(String name, DiscountType discountType, int discountAmount, int totalQuantity, LocalDateTime startsAt, LocalDateTime endsAt) {
		return new CouponEvent(name, discountType, discountAmount, totalQuantity, startsAt, endsAt);
	}

	// 쿠폰 발급 가능 여부 확인 메서드
	public boolean isIssuable(LocalDateTime now) {
		return status == CouponEventStatus.OPEN && !now.isBefore(startsAt) && !now.isAfter(endsAt);
	}

	// 쿠폰 이벤트 종료 메서드
	public void close() {
		this.status = CouponEventStatus.CLOSED;
	}

	// 상품 총액 기준으로 실제 할인 금액을 계산하는 메서드
	public int calculateDiscountAmount(int productAmount) {
		if (discountType == DiscountType.PERCENT) {
			return productAmount * discountAmount / 100;
		}

		return discountAmount;
	}
}
