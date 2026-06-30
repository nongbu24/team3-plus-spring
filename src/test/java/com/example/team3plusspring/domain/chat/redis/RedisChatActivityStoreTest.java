package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.port.ActiveChatSession;
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
import static org.mockito.Mockito.lenient;
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
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisChatActivityStore = new RedisChatActivityStore(redisTemplate);
    }

    @Test
    void 방활동을갱신하면_마지막활동시각을저장하고_경고상태를초기화한다() {
        // when
        redisChatActivityStore.refreshRoomActivity(1L);

        // then
        verify(valueOperations).set(eq("chat:activity:room:1"), anyString(), eq(Duration.ofMinutes(10)));
        verify(redisTemplate).delete("chat:activity:warning:room:1");
        verify(redisTemplate).keys("chat:activity:expired:room:1:user:*");
    }

    @Test
    void Redis저장소는_서버간공유활동정보를보존한다() {
        // when & then
        assertThat(redisChatActivityStore.preservesSharedRoomActivity()).isTrue();
    }

    @Test
    void 경고대상방은_경고마커를처음저장한서버에서만_반환한다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(5).toMillis();

        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.setIfAbsent("chat:activity:warning:room:1", "1", Duration.ofMinutes(10))).thenReturn(true);

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
        when(valueOperations.setIfAbsent("chat:activity:warning:room:1", "1", Duration.ofMinutes(10))).thenReturn(false);

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
        when(valueOperations.setIfAbsent(
                eq("chat:activity:expired:room:1:user:10"),
                eq(String.valueOf(oldActivityAt)),
                any(Duration.class)
        ))
                .thenReturn(true);

        // when
        Set<ActiveChatSession> sessions = redisChatActivityStore.findAndClaimExpiredSessions(
                Set.of(new ActiveChatSession(10L, 1L)),
                Duration.ofMinutes(5)
        );

        // then
        assertThat(sessions).containsExactly(new ActiveChatSession(10L, 1L));
    }

    @Test
    void 다른서버가이미만료처리를선점한방은_다시만료처리하지않는다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(6).toMillis();

        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.setIfAbsent(
                eq("chat:activity:expired:room:1:user:10"),
                eq(String.valueOf(oldActivityAt)),
                any(Duration.class)
        ))
                .thenReturn(false);

        // when
        Set<ActiveChatSession> sessions = redisChatActivityStore.findAndClaimExpiredSessions(
                Set.of(new ActiveChatSession(10L, 1L)),
                Duration.ofMinutes(5)
        );

        // then
        assertThat(sessions).isEmpty();
    }

    @Test
    void 만료선점후_마지막활동시각이같으면_선점이유효하다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(6).toMillis();

        when(valueOperations.get("chat:activity:expired:room:1:user:10")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(oldActivityAt));

        // when
        boolean valid = redisChatActivityStore.isExpiredClaimStillValid(new ActiveChatSession(10L, 1L));

        // then
        assertThat(valid).isTrue();
    }

    @Test
    void 만료선점후_마지막활동시각이바뀌면_선점이무효하다() {
        // given
        long oldActivityAt = System.currentTimeMillis() - Duration.ofMinutes(6).toMillis();
        long refreshedActivityAt = System.currentTimeMillis();

        when(valueOperations.get("chat:activity:expired:room:1:user:10")).thenReturn(String.valueOf(oldActivityAt));
        when(valueOperations.get("chat:activity:room:1")).thenReturn(String.valueOf(refreshedActivityAt));

        // when
        boolean valid = redisChatActivityStore.isExpiredClaimStillValid(new ActiveChatSession(10L, 1L));

        // then
        assertThat(valid).isFalse();
    }

    @Test
    void 만료선점후_선점키가삭제되면_선점이무효하다() {
        // given
        when(valueOperations.get("chat:activity:expired:room:1:user:10")).thenReturn(null);

        // when
        boolean valid = redisChatActivityStore.isExpiredClaimStillValid(new ActiveChatSession(10L, 1L));

        // then
        assertThat(valid).isFalse();
    }
}
