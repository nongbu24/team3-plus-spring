package com.example.team3plusspring.domain.coupon.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_coupons", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coupon_event_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "coupon_event_id", nullable = false)
	private Long couponEventId;

	@Column(name = "order_id")
	private Long orderId;

	@Column(nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private UserCouponStatus status;

	@Column(name = "issued_at", nullable = false)
	private LocalDateTime issuedAt;

	@Column(name = "used_at")
	private LocalDateTime usedAt;

	@Column(name = "expired_at")
	private LocalDateTime expiredAt;

	private UserCoupon(Long userId, Long couponEventId) {
		this.userId = userId;
		this.couponEventId = couponEventId;
		this.status = UserCouponStatus.ISSUED;
		this.issuedAt = LocalDateTime.now();
	}

	public static UserCoupon issue(Long userId, Long couponEventId) {
		return new UserCoupon(userId, couponEventId);
	}

	// 쿠폰 사용 처리 메서드
	public void markAsUsed(Long orderId) {
		if (status != UserCouponStatus.ISSUED) {
			throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
		}

		this.status = UserCouponStatus.USED;
		this.usedAt = LocalDateTime.now();
		this.orderId = orderId;
	}

	// 쿠폰 사용 취소(복구) 메서드
	public void restore() {
		if (status != UserCouponStatus.USED) {
			throw new BusinessException(ErrorCode.COUPON_NOT_USED);
		}

		this.status = UserCouponStatus.ISSUED;
		this.usedAt = null;
		this.orderId = null;
	}
}
