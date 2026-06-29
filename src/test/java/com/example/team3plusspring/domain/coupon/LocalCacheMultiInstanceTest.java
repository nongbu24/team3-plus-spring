package com.example.team3plusspring.domain.coupon;

import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import com.github.benmanes.caffeine.cache.Caffeine;

public class LocalCacheMultiInstanceTest {

	@Test
	void 인스턴스가_여러개면_한쪽_무효화가_다른쪽에_반영되지_않는다() {

		CaffeineCacheManager instanceA = new CaffeineCacheManager("couponEvents");
		instanceA.setCaffeine(Caffeine.newBuilder());
		CaffeineCacheManager instanceB = new CaffeineCacheManager("couponEvents");
		instanceB.setCaffeine(Caffeine.newBuilder());

		// 두 인스턴스 모두 발급 전 같은 조회 결과로 캐시가 채워진 상태라고 가장
		instanceA.getCache("couponEvents").put("0-10", "issuedQuantity=0");
		instanceB.getCache("couponEvents").put("0-10", "issuedQuantity=0");

		// 인스턴스 A로 발급 요청이 들어와서, A의 캐시만 무효화됨
		instanceA.getCache("couponEvents").evict("0-10");

		System.out.println("인스턴스A 캐시: " + instanceA.getCache("couponEvents").get("0-10"));
		System.out.println("인스턴스B 캐시: " + instanceB.getCache("couponEvents").get("0-10"));
	}
}
