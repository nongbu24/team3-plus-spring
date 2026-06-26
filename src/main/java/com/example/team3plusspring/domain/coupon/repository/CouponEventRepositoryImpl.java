package com.example.team3plusspring.domain.coupon.repository;

import static com.example.team3plusspring.domain.coupon.entity.QCouponEvent.couponEvent;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CouponEventRepositoryImpl implements CouponEventRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public long increaseIssuedQuantity(Long id) {
		return queryFactory.update(couponEvent)
			.set(couponEvent.issuedQuantity, couponEvent.issuedQuantity.add(1))
			.where(
				couponEvent.id.eq(id),
				couponEvent.issuedQuantity.lt(couponEvent.totalQuantity)
			)
			.execute();
	}
}
