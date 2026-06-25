package com.example.team3plusspring.global.lock;

import java.util.UUID;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @RedisLock이 붙은 메서드를 감싸서 분산 락을 적용하는 AOP
 * @Order(0)으로 설정하여 @Transactional보다 바깥쪽에서 동작하도록 한다
 * (락 획득 → 트랜잭션 시작·커밋 → 락 해제 순서를 보장해야, 커밋 전에 락이 풀려
 * 다음 요청이 아직 반영 안 된 데이터를 읽는 상황을 막을 수 있다)
 */

@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class RedisLockAspect {

	private final RedisLockService lockService;

	/**
	 * @RedisLock이 붙은 메서드 실행을 가로채서 락을 걸고, 메서드 실행 후 락을 해제한다
	 * 락 획득에 실패하면 메서드를 실행하지 않고 즉시 예외를 던진다(재시도하지 않음)
	 *
	 * @param joinPoint 가로챈 메서드 호출 정보
	 * @param redisLock 메서드에 붙은 @RedisLock 어노테이션 정보
	 * @return 원본 메서드의 실행 결과
	 */

	@Around("@annotation(redisLock)")
	public Object run(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {

		String value = UUID.randomUUID().toString();
		Object lockTarget = joinPoint.getArgs()[redisLock.argIndex()];
		String key = redisLock.key() + lockTarget;

		boolean locked = lockService.tryLock(key, value, redisLock.timeout());

		if (!locked) {
			log.info("락 획득 실패 : {}", Thread.currentThread().getName());
			throw new BusinessException(ErrorCode.COUPON_ISSUE_LOCK_FAILED);
		}

		try {
			log.info("락 획득 성공 : {}", Thread.currentThread().getName());
			return joinPoint.proceed();
		} finally {
			lockService.unlock(key, value);
		}
	}
}
