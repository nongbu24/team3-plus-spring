package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.service.ChatActivityStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class RedisChatActivityStore implements ChatActivityStore {
    private static final String LAST_ACTIVITY_KEY_PREFIX = "chat:activity:room:";
    private static final String WARNING_KEY_PREFIX = "chat:activity:warning:room:";
    private static final String EXPIRED_KEY_PREFIX = "chat:activity:expired:room:";
    private static final String MISSING_LAST_ACTIVITY_VALUE = "missing";
    private static final Duration EXPIRED_CLAIM_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void refreshRoomActivity(Long roomId) {
        String now = String.valueOf(Instant.now().toEpochMilli());

        redisTemplate.opsForValue().set(lastActivityKey(roomId), now);
        redisTemplate.delete(warningKey(roomId));
        deleteExpiredKeys(roomId);
    }

    @Override
    public void removeRoomActivity(Long roomId) {
        redisTemplate.delete(lastActivityKey(roomId));
        redisTemplate.delete(warningKey(roomId));
        deleteExpiredKeys(roomId);
    }

    @Override
    public boolean preservesSharedRoomActivity() {
        return true;
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
    public Set<ActiveChatSession> findAndClaimExpiredSessions(Set<ActiveChatSession> activeSessions, Duration timeout) {
        long expiredThreshold = Instant.now().minus(timeout).toEpochMilli();

        return activeSessions.stream()
                .filter(activeSession -> claimExpiredIfStillExpired(activeSession, expiredThreshold))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isExpiredClaimStillValid(ActiveChatSession activeSession) {
        String claimedLastActivityAt = redisTemplate.opsForValue().get(expiredKey(activeSession));

        if (claimedLastActivityAt == null) {
            return false;
        }

        return Objects.equals(claimedLastActivityAt, claimedLastActivityValue(activeSession.roomId()));
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

    private boolean claimExpiredIfStillExpired(ActiveChatSession activeSession, long expiredThreshold) {
        String lastActivityAt = lastActivityValue(activeSession.roomId());

        if (!isExpired(lastActivityAt, expiredThreshold)) {
            return false;
        }

        Boolean claimed = redisTemplate.opsForValue()
                .setIfAbsent(expiredKey(activeSession), claimedLastActivityValue(lastActivityAt), EXPIRED_CLAIM_TTL);

        return Boolean.TRUE.equals(claimed);
    }

    private boolean isExpired(String lastActivityAt, long expiredThreshold) {
        if (lastActivityAt == null) {
            return true;
        }

        return Long.parseLong(lastActivityAt) <= expiredThreshold;
    }

    private Long lastActivityAt(Long roomId) {
        String value = lastActivityValue(roomId);

        if (value == null) {
            return null;
        }

        return Long.parseLong(value);
    }

    private String lastActivityValue(Long roomId) {
        return redisTemplate.opsForValue().get(lastActivityKey(roomId));
    }

    private String claimedLastActivityValue(Long roomId) {
        return claimedLastActivityValue(lastActivityValue(roomId));
    }

    private String claimedLastActivityValue(String lastActivityAt) {
        if (lastActivityAt == null) {
            return MISSING_LAST_ACTIVITY_VALUE;
        }

        return lastActivityAt;
    }

    private void deleteExpiredKeys(Long roomId) {
        Set<String> expiredKeys = redisTemplate.keys(expiredKeyPattern(roomId));

        if (expiredKeys == null || expiredKeys.isEmpty()) {
            return;
        }

        redisTemplate.delete(expiredKeys);
    }

    private String lastActivityKey(Long roomId) {
        return LAST_ACTIVITY_KEY_PREFIX + roomId;
    }

    private String warningKey(Long roomId) {
        return WARNING_KEY_PREFIX + roomId;
    }

    private String expiredKeyPattern(Long roomId) {
        return EXPIRED_KEY_PREFIX + roomId + ":user:*";
    }

    private String expiredKey(ActiveChatSession activeSession) {
        return EXPIRED_KEY_PREFIX + activeSession.roomId() + ":user:" + activeSession.userId();
    }
}
