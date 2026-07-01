package com.example.team3plusspring.domain.coupon.repository;

import static com.example.team3plusspring.domain.coupon.entity.QUserCoupon.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.entity.UserCouponStatus;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<UserCoupon> findUsableUserCoupons(Long userId, LocalDateTime now, Pageable pageable) {
		Predicate usable = userCoupon.userId.eq(userId)
			.and(userCoupon.status.eq(UserCouponStatus.ISSUED))
			.and(userCoupon.expiredAt.isNull().or(userCoupon.expiredAt.goe(now)));

		List<UserCoupon> content = queryFactory
			.selectFrom(userCoupon)
			.where(usable)
			.orderBy(userCoupon.issuedAt.desc(), userCoupon.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		long total = queryFactory
			.select(userCoupon.count())
			.from(userCoupon)
			.where(usable)
			.fetchOne();

		return new PageImpl<>(content, pageable, total);
	}

	@Override
	public boolean existsByUserIdAndCouponEventId(Long userId, Long couponEventId) {
		return queryFactory
			.selectOne()
			.from(userCoupon)
			.where(
				userCoupon.userId.eq(userId),
				userCoupon.couponEventId.eq(couponEventId)
			)
			.fetchFirst() != null;
	}

	@Override
	public Optional<UserCoupon> findByIdAndUserIdForUpdate(Long userCouponId, Long userId) {
		return Optional.ofNullable(
			queryFactory
				.selectFrom(userCoupon)
				.where(
					userCoupon.id.eq(userCouponId),
					userCoupon.userId.eq(userId)
				)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}

	@Override
	public long countByCouponEventId(Long couponEventId) {
		Long count = queryFactory
			.select(userCoupon.count())
			.from(userCoupon)
			.where(userCoupon.couponEventId.eq(couponEventId))
			.fetchOne();

		return count != null ? count : 0L;
	}

	@Override
	public Optional<UserCoupon> findByOrderIdForUpdate(Long orderId) {
		return Optional.ofNullable(
			queryFactory
				.selectFrom(userCoupon)
				.where(userCoupon.orderId.eq(orderId))
				.setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.fetchOne()
		);
	}
}
