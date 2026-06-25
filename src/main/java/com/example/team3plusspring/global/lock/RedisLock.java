package com.example.team3plusspring.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드에 분산 락을 적용하기 위한 어노테이션
 * 메서드 실행 전 Redis에 락을 걸고, 실행이 끝나면(정상/예외 상관없이) 락을 해제한다.
 * 동시에 같은 락 키로 들어온 다른 요청은 락 획득에 실패하며 즉시 예외가 발생한다.(fail-fast 전략)
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisLock {

	/**
	 * 락 키의 접두사. 실제 락 키는 "key + 락 대상 파라미터 값" 으로 만들어진다.
	 */
	String key();

	/**
	 * 락 대상이 되는 값이 메서드 파라미터 중 몇 번째(0부터 시작)인지 지정한다.
	 * 예) issueCoupon(Long userId, Long couponEventId)에서 couponEventId를 락 대상으로 쓰려면 1
	 */
	int argIndex() default 0;

	/**
	 * 락의 유효시간(초). 이 시간이 지나면 Redis에서 자동으로 키가 삭제된다.
	 * 서버 장애로 락 해제가 안 되는 상황을 방지하기 위한 안전장치
	 */
	long timeout() default 5;
}
