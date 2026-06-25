package com.example.team3plusspring.global.lock;

import java.time.Duration;
import java.util.Collections;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis의 SETNX(Set if Not Exists) 명령을 이용해 분산 락을 획득/해제하는 서비스
 * 락의 소유권은 value로 구분하여, 락을 건 주체만 해제할 수 있도록 한다.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLockService {

	private final StringRedisTemplate redisTemplate;

	/**
	 * 락 획득을 시도하는 메서드
	 * 이미 같은 key로 락이 걸려있으면 실패하며, 성공 시 timeoutSeconds 후 자동 만료되는 락을 건다
	 *
	 * @param key 락 키
	 * @param value 락 소유자를 식별하는 값(UUID)
	 * @param timeoutSeconds 락 유효시간(초)
	 * @return 락 획득 성공 여부
	 */

	public boolean tryLock(String key, String value, long timeoutSeconds) {
		Boolean result = redisTemplate.opsForValue()
			.setIfAbsent(key, value, Duration.ofSeconds(timeoutSeconds));
		return Boolean.TRUE.equals(result);
	}

	/**
	 * 락을 해제하는 메서드
	 * Lua 스크립트로 "현재 저장된 값이 내가 건 값과 같을 때만 삭제"를 원자적으로 수행하여,
	 * 락이 만료된 후 다른 스레드가 새로 건 락을 실수로 해제하는 것을 방지한다
	 *
	 * @param key 락 키
	 * @param value 락 소유자를 식별하는 값
	 */

	public void unlock(String key, String value) {
		String script =
			"if redis.call('get', KEYS[1]) == ARGV[1] then "
			+ "   return redis.call('del', KEYS[1]) "
			+ "else "
			+ "   return 0 "
			+ "end";

		redisTemplate.execute(
			new DefaultRedisScript<>(script, Long.class),
			Collections.singletonList(key),
			value
		);

		log.info("획득한 락 키 반납 :::: {}", Thread.currentThread().getName());
	}
}
