package com.example.team3plusspring.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트 실행 시 Redis 컨테이너를 자동으로 띄워서, 로컬에 미리 Redis를 띄워두지 않아도
 * RedissonClient 빈이 정상적으로 생성되도록 한다.
 * RedissonClient를 사용하는(또는 전체 ApplicationContext를 로드하는) 테스트는 이 클래스를 상속받으면 된다.
 */
@Testcontainers
public abstract class RedisTestContainerSupport {

	@Container
	static GenericContainer<?> REDIS_CONTAINER =
		new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void registerRedisProperties(DynamicPropertyRegistry registry) {
		registry.add("REDIS_HOST", REDIS_CONTAINER::getHost);
		registry.add("REDIS_PORT", () -> REDIS_CONTAINER.getMappedPort(6379));
	}
}
