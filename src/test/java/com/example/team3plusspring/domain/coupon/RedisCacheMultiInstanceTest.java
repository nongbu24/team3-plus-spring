package com.example.team3plusspring.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

public class RedisCacheMultiInstanceTest {

	@Test
	void 인스턴스가_여러개여도_한쪽_무효화가_다른쪽에도_반영된다() {

		LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
			new RedisStandaloneConfiguration("localhost", 6379));
		connectionFactory.afterPropertiesSet();

		RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
				GenericJacksonJsonRedisSerializer.builder().enableUnsafeDefaultTyping().build()));

		// 같은 Redis를 보는 두 인스턴스를 흉내냄
		RedisCacheManager instanceA = RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(cacheConfiguration).build();
		RedisCacheManager instanceB = RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(cacheConfiguration).build();

		instanceA.getCache("couponEvents").put("0-10", "issuedQuantity=0");

		System.out.println("인스턴스B 캐시(A가 쓴 직후): " + instanceB.getCache("couponEvents").get("0-10"));
		assertThat(instanceB.getCache("couponEvents").get("0-10")).isNotNull();

		instanceA.getCache("couponEvents").evict("0-10");

		System.out.println("인스턴스A 캐시: " + instanceA.getCache("couponEvents").get("0-10"));
		System.out.println("인스턴스B 캐시: " + instanceB.getCache("couponEvents").get("0-10"));
		assertThat(instanceA.getCache("couponEvents").get("0-10")).isNull();
		assertThat(instanceB.getCache("couponEvents").get("0-10")).isNull();
	}
}
