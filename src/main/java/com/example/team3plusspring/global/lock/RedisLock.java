package com.example.team3plusspring.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드에 분산 락(Redisson 공정 락)을 적용하기 위한 어노테이션
 * 락을 잡으려는 스레드들을 대기 큐에 도착한 순서대로 줄 세워서,
 * 스핀 락처럼 운에 따라 아무나 락을 잡는 게 아니라 "먼저 온 요청이 먼저 처리되는 것"을 보장한다.
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
	 * 락을 기다릴 수 있는 최대 시간(초). 이 시간 안에 내 차례가 안 오면 포기한다.
	 */
	long waitTime() default 5;

	/**
	 * 락을 획득한 뒤 최대로 들고 있을 수 있는 시간(초). 이 시간이 지나면 자동으로 풀린다.
	 */
	long leaseTime() default 3;
}
