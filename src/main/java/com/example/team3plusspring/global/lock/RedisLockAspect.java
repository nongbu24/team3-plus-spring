package com.example.team3plusspring.global.lock;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @RedisLock이 붙은 메서드를 감싸서 Redisson 공정 락을 적용하는 AOP
 * @Order(0)으로 설정하여 @Transactional보다 바깥쪽에서 동작하도록 한다
 */

@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class RedisLockAspect {

	private final RedissonClient redissonClient;

	@Around("@annotation(redisLock)")
	public Object run(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {

		Object lockTarget = joinPoint.getArgs()[redisLock.argIndex()];
		String key = redisLock.key() + lockTarget;
		RLock lock = redissonClient.getFairLock(key);

		boolean locked = false;

		try {
			locked = lock.tryLock(redisLock.waitTime(), redisLock.leaseTime(), TimeUnit.SECONDS);

			if (!locked) {
				log.info("락 획득 실패: {}", Thread.currentThread().getName());
				throw new BusinessException(ErrorCode.COUPON_ISSUE_LOCK_FAILED);
			}

			log.info("락 획득 성공: {}", Thread.currentThread().getName());
			return joinPoint.proceed();
		} finally {
			if (locked && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}
