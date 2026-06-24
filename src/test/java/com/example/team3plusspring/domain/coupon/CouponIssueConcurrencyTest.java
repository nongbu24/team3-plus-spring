package com.example.team3plusspring.domain.coupon;

import java.time.LocalDateTime;
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
	void 동시에_쿠폰발급_요청한다() {

		// given
		CouponEvent couponEvent = couponEventRepository.save(
			CouponEvent.create(
				"동시성 테스트 쿠폰",
				DiscountType.FIXED,
				1000,
				2,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1)
			)
		);

		ExecutorService executor = Executors.newFixedThreadPool(3);

		Runnable task1 = () -> {
			try {
				couponEventService.issueCoupon(1L, couponEvent.getId());
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " 실패: " + e.getMessage());
			}
		};

		Runnable task2 = () -> {
			try {
				couponEventService.issueCoupon(2L, couponEvent.getId());
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " 실패: " + e.getMessage());
			}
		};

		Runnable task3 = () -> {
			try {
				couponEventService.issueCoupon(3L, couponEvent.getId());
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " 실패: " + e.getMessage());
			}
		};

		// when
		executor.submit(task1);
		executor.submit(task2);
		executor.submit(task3);

		executor.shutdown();

		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// then
		CouponEvent result = couponEventRepository.findById(couponEvent.getId()).orElseThrow();
		long actualIssuedCount = userCouponRepository.countByCouponEventId(couponEvent.getId());

		System.out.println("CouponEvent.issuedQuantity 컬럼 값: " + result.getIssuedQuantity());
		System.out.println("실제 생성된 UserCoupon 행 수: " + actualIssuedCount);
	}
}
