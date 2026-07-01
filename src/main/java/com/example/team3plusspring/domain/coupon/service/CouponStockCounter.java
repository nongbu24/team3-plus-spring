package com.example.team3plusspring.domain.coupon.service;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponStockCounter {

	private static final String KEY_PREFIX = "coupon:stock:";
	private static final long STOCK_NOT_INITIALIZED = -2L;

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
		Long result = redisTemplate.execute(DECREASE_SCRIPT, List.of(key(couponEventId)));

		if (result == null || result == STOCK_NOT_INITIALIZED) {
			log.warn("쿠폰 재고 키가 Redis에 존재하지 않음. couponEventId={}", couponEventId);
			throw new BusinessException(ErrorCode.COUPON_STOCK_NOT_INITIALIZED);
		}

		return result >= 0;
	}

	public void restoreStock(Long couponEventId) {
		redisTemplate.opsForValue().increment(key(couponEventId));
	}

	private String key(Long couponEventId) {
		return KEY_PREFIX + couponEventId;
	}

	public long getStock(Long couponEventId) {
		String value = redisTemplate.opsForValue().get(key(couponEventId));
		return value != null ? Long.parseLong(value) : 0L;
	}
}
