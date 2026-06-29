package com.example.team3plusspring.domain.chat.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatActivityStoreTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    RedisChatActivityStore redisChatActivityStore;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisChatActivityStore = new RedisChatActivityStore(redisTemplate);
    }

    @Test
    void 방활동을갱신하면_마지막활동시각을저장하고_경고상태를초기화한다() {
        // when
        redisChatActivityStore.refreshRoomActivity(1L);

        // then
        verify(valueOperations).set(eq("chat:activity:room:1"), anyString());
        verify(redisTemplate).delete("chat:activity:warning:room:1");
        verify(redisTemplate).delete("chat:activity:expired:room:1");
    }

    @Test
    void 경고대상방은_경고마커를처음저장한서버에서만_반환한다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(5).toMillis();

        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.setIfAbsent("chat:activity:warning:room:1", "1")).thenReturn(true);

        // when
        Set<Long> roomIds = redisChatActivityStore.findAndMarkWarningRoomIds(
                Set.of(1L),
                Duration.ofMinutes(4).plusSeconds(30)
        );

        // then
        assertThat(roomIds).containsExactly(1L);
    }

    @Test
    void 다른서버가이미비활성경고를보낸방은_다시경고하지않는다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(5).toMillis();

        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.setIfAbsent("chat:activity:warning:room:1", "1")).thenReturn(false);

        // when
        Set<Long> roomIds = redisChatActivityStore.findAndMarkWarningRoomIds(
                Set.of(1L),
                Duration.ofMinutes(4).plusSeconds(30)
        );

        // then
        assertThat(roomIds).isEmpty();
    }

    @Test
    void 만료대상방은_만료마커를처음저장한서버에서만_반환한다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(6).toMillis();

        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.setIfAbsent(eq("chat:activity:expired:room:1"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        // when
        Set<Long> roomIds = redisChatActivityStore.findAndClaimExpiredRoomIds(
                Set.of(1L),
                Duration.ofMinutes(5)
        );

        // then
        assertThat(roomIds).containsExactly(1L);
    }

    @Test
    void 다른서버가이미만료처리를선점한방은_다시만료처리하지않는다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(6).toMillis();

        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.setIfAbsent(eq("chat:activity:expired:room:1"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        // when
        Set<Long> roomIds = redisChatActivityStore.findAndClaimExpiredRoomIds(
                Set.of(1L),
                Duration.ofMinutes(5)
        );

        // then
        assertThat(roomIds).isEmpty();
    }
}
