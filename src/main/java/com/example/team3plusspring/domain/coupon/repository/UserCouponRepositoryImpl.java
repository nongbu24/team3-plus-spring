package com.example.team3plusspring.domain.coupon.repository;

import static com.example.team3plusspring.domain.coupon.entity.QUserCoupon.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.team3plusspring.domain.coupon.entity.UserCoupon;
import com.example.team3plusspring.domain.coupon.entity.UserCouponStatus;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQueryFactory;

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
}
