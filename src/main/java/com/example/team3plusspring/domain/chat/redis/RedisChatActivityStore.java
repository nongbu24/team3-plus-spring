package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.service.ChatActivityStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class RedisChatActivityStore implements ChatActivityStore {
    private static final String LAST_ACTIVITY_KEY_PREFIX = "chat:activity:room:";
    private static final String WARNING_KEY_PREFIX = "chat:activity:warning:room:";
    private static final String EXPIRED_KEY_PREFIX = "chat:activity:expired:room:";
    private static final Duration EXPIRED_CLAIM_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void refreshRoomActivity(Long roomId) {
        String now = String.valueOf(Instant.now().toEpochMilli());

        redisTemplate.opsForValue().set(lastActivityKey(roomId), now);
        redisTemplate.delete(warningKey(roomId));
        redisTemplate.delete(expiredKey(roomId));
    }

    @Override
    public void removeRoomActivity(Long roomId) {
        redisTemplate.delete(lastActivityKey(roomId));
        redisTemplate.delete(warningKey(roomId));
        redisTemplate.delete(expiredKey(roomId));
    }

    @Override
    public Set<Long> findAndMarkWarningRoomIds(Set<Long> roomIds, Duration warningAfter) {
        long warningThreshold = Instant.now().minus(warningAfter).toEpochMilli();

        return roomIds.stream()
                .filter(roomId -> needsWarning(roomId, warningThreshold))
                .filter(this::markWarningIfAbsent)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> findAndClaimExpiredRoomIds(Set<Long> roomIds, Duration timeout) {
        long expiredThreshold = Instant.now().minus(timeout).toEpochMilli();

        return roomIds.stream()
                .filter(roomId -> isExpired(roomId, expiredThreshold))
                .filter(this::claimExpiredIfAbsent)
                .collect(Collectors.toSet());
    }

    private boolean needsWarning(Long roomId, long warningThreshold) {
        Long lastActivityAt = lastActivityAt(roomId);

        return lastActivityAt != null && lastActivityAt <= warningThreshold;
    }

    private boolean markWarningIfAbsent(Long roomId) {
        Boolean marked = redisTemplate.opsForValue()
                .setIfAbsent(warningKey(roomId), "1");

        return Boolean.TRUE.equals(marked);
    }

    private boolean claimExpiredIfAbsent(Long roomId) {
        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(expiredKey(roomId), "1", EXPIRED_CLAIM_TTL);

        return Boolean.TRUE.equals(claimed);
    }

    private boolean isExpired(Long roomId, long expiredThreshold) {
        Long lastActivityAt = lastActivityAt(roomId);

        return lastActivityAt == null || lastActivityAt <= expiredThreshold;
    }

    private Long lastActivityAt(Long roomId) {
        String value = redisTemplate.opsForValue().get(lastActivityKey(roomId));

        if (value == null) {
            return null;
        }

        return Long.parseLong(value);
    }

    private String lastActivityKey(Long roomId) {
        return LAST_ACTIVITY_KEY_PREFIX + roomId;
    }

    private String warningKey(Long roomId) {
        return WARNING_KEY_PREFIX + roomId;
    }

    private String expiredKey(Long roomId) {
        return EXPIRED_KEY_PREFIX + roomId;
    }
}
