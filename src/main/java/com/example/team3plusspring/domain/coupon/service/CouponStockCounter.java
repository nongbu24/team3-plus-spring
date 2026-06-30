package com.example.team3plusspring.domain.coupon.service;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponStockCounter {

	private static final String KEY_PREFIX = "coupon:stock:";

	private static final RedisScript<Long> DECREASE_SCRIPT = RedisScript.of(
	"""
		local stock = redis.call('GET', KEYS[1])
		if stock == false then
			return -2
		end
		if tonumber(stock) <= 0 then
			return -1
		end
		return redis.call('DECR', KEYS[1])
		""",
		Long.class
	);

	private final StringRedisTemplate redisTemplate;

	public void initStock(Long couponEventId, int totalQuantity) {
		redisTemplate.opsForValue().set(key(couponEventId), String.valueOf(totalQuantity));
	}

	public boolean decreaseStock(Long couponEventId) {
		Long remaining = redisTemplate.execute(DECREASE_SCRIPT, List.of(key(couponEventId)));
		return remaining != null && remaining >= 0;
	}

	public void restoreStock(Long couponEventId) {
		redisTemplate.opsForValue().increment(key(couponEventId));
	}

	private String key(Long couponEventId) {
		return KEY_PREFIX + couponEventId;
	}
}
