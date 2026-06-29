package com.example.team3plusspring.domain.coupon;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;

import com.example.team3plusspring.domain.coupon.dto.GetCouponEventListResponse;
import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.DiscountType;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.service.CouponEventService;
import com.example.team3plusspring.support.RedisTestSupport;

import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
public class CouponEventCachingTest extends RedisTestSupport {

	@Autowired
	private CouponEventService couponEventService;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private CouponEventRepository couponEventRepository;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void clearCache() {
		cacheManager.getCache("couponEvents").clear();
	}

	@Test
	void 캐시가_없으면_동시조회_횟수만큼_쿼리가_반복_실행된다() throws InterruptedException {

		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();

		int requestCount = 1000;
		ExecutorService executorService = Executors.newFixedThreadPool(32);

		long start = System.currentTimeMillis();
		for (int i = 0; i < requestCount; i++) {
			executorService.submit(() -> couponEventService.getCouponEvents(0, 10));
		}
		executorService.shutdown();;
		executorService.awaitTermination(10, TimeUnit.SECONDS);
		long elapsed = System.currentTimeMillis() - start;

		System.out.println("요청 수: " + requestCount);
		System.out.println("총 소요시간: " + elapsed + "ms");
		System.out.println("실행된 쿼리 수: " + statistics.getQueryExecutionCount());
	}

	@Test
	void 캐시_적용후_발급하면_목록_조회결과의_발급수량이_즉시_반영된다() {

		CouponEvent couponEvent = couponEventRepository.save(
			CouponEvent.create(
				"캐싱테스트쿠폰",
				DiscountType.FIXED,
				1000,
				10,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1),
				7
				)
		);

		Page<GetCouponEventListResponse> before = couponEventService.getCouponEvents(0, 10);
		int issuedQuantityBeforeIssue = before.getContent().stream()
			.filter(r -> r.getId().equals(couponEvent.getId()))
			.findFirst().orElseThrow().getIssuedQuantity();

		couponEventService.issueCoupon(1L, couponEvent.getId());

		CouponEvent actual = couponEventRepository.findById(couponEvent.getId()).orElseThrow();
		System.out.println("실제 DB의 issuedQuantity: " + actual.getIssuedQuantity());

		Page<GetCouponEventListResponse> after = couponEventService.getCouponEvents(0, 10);
		int issuedQuantityAfterIssue = after.getContent().stream()
			.filter(r -> r.getId().equals(couponEvent.getId()))
			.findFirst().orElseThrow().getIssuedQuantity();

		System.out.println("발급 전: " + issuedQuantityBeforeIssue);
		System.out.println("발급 후: " + issuedQuantityAfterIssue);
	}

	@Test
	void 발급이_잦아지면_캐시_적중률이_떨어진다() {

		CouponEvent couponEvent = couponEventRepository.save(
			CouponEvent.create(
				"hit율테스트쿠폰",
				DiscountType.FIXED,
				1000,
				1000,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1),
				7
			)
		);

		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.setStatisticsEnabled(true);
		statistics.clear();

		for (long userId = 1; userId <= 100; userId++) {
			couponEventService.getCouponEvents(0, 10);
			couponEventService.issueCoupon(userId, couponEvent.getId());
		}

		System.out.println("조회+발급 100세트 동안 실행된 쿼리 수: " + statistics.getQueryExecutionCount());
	}
}
