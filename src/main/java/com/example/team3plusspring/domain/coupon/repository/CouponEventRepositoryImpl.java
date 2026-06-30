package com.example.team3plusspring.domain.coupon.repository;

import static com.example.team3plusspring.domain.coupon.entity.QCouponEvent.couponEvent;

import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.CouponEventStatus;
import com.querydsl.core.types.Predicate;
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

	@Override
	public Page<CouponEvent> findOpenCouponEvents(LocalDateTime now, Pageable pageable) {
		Predicate openAndActive = couponEvent.status.eq(CouponEventStatus.OPEN)
			.and(couponEvent.startsAt.loe(now))
			.and(couponEvent.endsAt.goe(now));

		List<CouponEvent> content = queryFactory
			.selectFrom(couponEvent)
			.where(openAndActive)
			.orderBy(couponEvent.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		long total = queryFactory
			.select(couponEvent.count())
			.from(couponEvent)
			.where(openAndActive)
			.fetchOne();

		return new PageImpl<>(content, pageable, total);
	}
}
