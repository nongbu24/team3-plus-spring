package com.example.team3plusspring.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.team3plusspring.domain.coupon.entity.CouponEvent;
import com.example.team3plusspring.domain.coupon.entity.DiscountType;
import com.example.team3plusspring.domain.coupon.repository.CouponEventRepository;
import com.example.team3plusspring.domain.coupon.repository.UserCouponRepository;
import com.example.team3plusspring.domain.coupon.service.CouponEventService;
import com.example.team3plusspring.support.RedisTestContainerSupport;

@SpringBootTest
class CouponIssueConcurrencyTest extends RedisTestContainerSupport {

	@Autowired
	CouponEventService couponEventService;

	@Autowired
	CouponEventRepository couponEventRepository;

	@Autowired
	UserCouponRepository userCouponRepository;

	@Autowired
	RedissonClient redissonClient;

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
	void 분산_락은_도착한_순서대로_쿠폰을_발급한다() throws InterruptedException {

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

		String lockKey = "lock:coupon:" + couponEvent.getId();
		RLock blocker = redissonClient.getFairLock(lockKey);
		blocker.lock(); // 미리 락을 잡아서, 아래 스레드들이 전부 대기 줄에 서게 만든다

		List<Long> arrivalOrder = new CopyOnWriteArrayList<>();
		List<Long> successOrder = new CopyOnWriteArrayList<>();
		CountDownLatch latch = new CountDownLatch(threadCount);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);

		// when
		for (int i = 0; i < threadCount; i++) {
			long userId = i + 1;
			executor.submit(() -> {
				try {
					Thread.sleep(userId * 30); // userId가 클수록 늦게 도착하도록 의도적으로 지연
					arrivalOrder.add(userId); // 줄 서는 (tryLock 호출) 순서 기록
					couponEventService.issueCoupon(userId, couponEvent.getId());
					successOrder.add(userId); // 발급 성공한 순서 기록
				} catch (Exception e) {
					System.out.println(userId + "번 유저 실패: " + e.getMessage());
				} finally {
					latch.countDown();
				}
			});
		}

		Thread.sleep(500); // 10명 다 대기 줄에 들어갈 시간을 충분히 줌
		blocker.unlock(); // 이제 대기 줄이 풀리기 시작

		latch.await();
		executor.shutdown();

		//then
		System.out.println("도착(줄 선) 순서: " + arrivalOrder);
		System.out.println("발급 성공 순서: " + successOrder);

		assertThat(successOrder).containsExactly(1L, 2L);
	}
}
