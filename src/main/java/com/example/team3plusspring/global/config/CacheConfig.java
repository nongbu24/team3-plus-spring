package com.example.team3plusspring.global.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager("couponEvents");
		cacheManager.setCaffeine(
			Caffeine.newBuilder()
				.maximumSize(500)
				.expireAfterWrite(Duration.ofSeconds(30))
				.recordStats()
		);
		return cacheManager;
	}
}
