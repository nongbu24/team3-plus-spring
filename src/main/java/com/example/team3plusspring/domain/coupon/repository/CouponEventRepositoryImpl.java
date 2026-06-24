package com.example.team3plusspring.domain.coupon.repository;

import static com.example.team3plusspring.domain.coupon.entity.QCouponEvent.*;

import java.util.Optional;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CouponEventRepositoryImpl implements CouponEventRepositoryCustom {

	private final JPAQueryFactory queryFactory;

	@Override
	public Optional<CouponEvent> findByIdForUpdate(Long id) {

		CouponEvent result = queryFactory
			.selectFrom(couponEvent)
			.where(couponEvent.id.eq(id))
			.setLockMode(LockModeType.PESSIMISTIC_WRITE)
			.fetchOne();

		return Optional.ofNullable(result);
	}
}
