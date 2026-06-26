package com.example.team3plusspring.domain.coupon;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.team3plusspring.domain.coupon.service.CouponEventService;
import com.example.team3plusspring.support.RedisTestSupport;

import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
public class CouponEventCachingTest extends RedisTestSupport {

	@Autowired
	private CouponEventService couponEventService;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

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
}
