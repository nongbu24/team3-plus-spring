package com.example.team3plusspring.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.DiscountType;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.repository.UserCouponRepository;
import com.example.team3plusspring.domain.coupon.service.CouponEventService;
import com.example.team3plusspring.domain.coupon.service.CouponStockCounter;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.support.RedisTestSupport;

@SpringBootTest
class CouponIssueConcurrencyTest extends RedisTestSupport {

	@Autowired
	CouponEventService couponEventService;

	@Autowired
	CouponEventRepository couponEventRepository;

	@Autowired
	UserCouponRepository userCouponRepository;

	@Autowired
	CouponStockCounter couponStockCounter;

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
				LocalDateTime.now().plusDays(1),
				30
			)
		);
		couponStockCounter.initStock(couponEvent.getId(), totalQuantity);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		// when
		for (int i = 0; i < threadCount; i++) {
			long userId = i + 1;
			executor.submit(() -> {
				try {
					couponEventService.issueCoupon(userId, couponEvent.getId());
					successCount.incrementAndGet();
				} catch (Exception e) {
					failCount.incrementAndGet();
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

		System.out.println("성공: " + successCount.get() + ", 실패: " + failCount.get());
		System.out.println("실제 발급된 쿠폰 수: " + actualIssuedCount);

		assertThat(successCount.get()).isEqualTo(totalQuantity);
		assertThat(failCount.get()).isEqualTo(threadCount - totalQuantity);
		assertThat(actualIssuedCount).isEqualTo(totalQuantity);
		assertThat(result.getIssuedQuantity()).isEqualTo(actualIssuedCount);
	}

	@Test
	void 같은_유저가_동시에_발급요청하면_쿠폰은_1개만_생성되고_나머지는_중복발급_예외가_반환된다() throws InterruptedException {

		// given
		int totalQuantity = 100;
		int threadCount = 10;
		long sameUserId = 999L;

		CouponEvent couponEvent = couponEventRepository.save(
			CouponEvent.create(
				"중복발급 동시성 테스트 쿠폰",
				DiscountType.FIXED,
				1000,
				totalQuantity,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusDays(1),
				30
			)
		);
		couponStockCounter.initStock(couponEvent.getId(), totalQuantity);

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger alreadyIssuedCount = new AtomicInteger();
		AtomicInteger unexpectedErrorCount = new AtomicInteger();

		// when
		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					couponEventService.issueCoupon(sameUserId, couponEvent.getId());
					successCount.incrementAndGet();
				} catch (BusinessException e) {
					if (e.getErrorCode() == ErrorCode.COUPON_ALREADY_ISSUED) {
						alreadyIssuedCount.incrementAndGet();
					} else {
						unexpectedErrorCount.incrementAndGet();
					}
				} catch (Exception e) {
					unexpectedErrorCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await();
		executor.shutdown();

		//then
		CouponEvent result = couponEventRepository.findById(couponEvent.getId()).orElseThrow();
		long actualIssuedCount = userCouponRepository.countByCouponEventId(couponEvent.getId());
		long remainingStock = couponStockCounter.getStock(couponEvent.getId());

		System.out.println("성공: " + successCount.get()
		+ ", 중복발급 거절: " + alreadyIssuedCount.get()
		+ ", 예상치 못한 오류: " + unexpectedErrorCount.get());
		System.out.println("실제 발급된 쿠폰 수: " + actualIssuedCount);
		System.out.println("남은 Redis 재고: " + remainingStock);

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(alreadyIssuedCount.get()).isEqualTo(threadCount - 1);
		assertThat(unexpectedErrorCount.get()).isZero();
		assertThat(actualIssuedCount).isEqualTo(1);
		assertThat(result.getIssuedQuantity()).isEqualTo(1);
		assertThat(remainingStock).isEqualTo(totalQuantity - 1);
	}
}
