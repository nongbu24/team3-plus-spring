package com.example.team3plusspring.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.DiscountType;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.repository.UserCouponRepository;
import com.example.team3plusspring.domain.coupon.service.CouponEventService;

@SpringBootTest
class CouponIssueConcurrencyTest {

	@Autowired
	CouponEventService couponEventService;

	@Autowired
	CouponEventRepository couponEventRepository;

	@Autowired
	UserCouponRepository userCouponRepository;

	@Test
	void 동시에_쿠폰발급_요청하면_재고를_초과해서_발급되지_않는다() throws InterruptedException {

		// given
		int totalQuantity = 2;
		int threadCount = 10;

		CouponEvent couponEvent = couponEventRepository.save(
			CouponEvent.create(
				"동시성 테스트 쿠폰",
				DiscountType.FIXED,
				1000,
				totalQuantity,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1)
			)
		);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		// when
		for (int i = 0; i < threadCount; i++) {
			long userId = i + 1;
			executor.submit(() -> {
				try {
					couponEventService.issueCoupon(userId, couponEvent.getId());
				} catch (Exception e) {
					System.out.println(Thread.currentThread().getName() + " 실패: " + e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await();
		executor.shutdown();

		// then
		CouponEvent result = couponEventRepository.findById(couponEvent.getId()).orElseThrow();
		long actualIssuedCount = userCouponRepository.countByCouponEventId(couponEvent.getId());

		assertThat(actualIssuedCount).isEqualTo(totalQuantity);
		assertThat(result.getIssuedQuantity()).isEqualTo(actualIssuedCount);
	}
}
