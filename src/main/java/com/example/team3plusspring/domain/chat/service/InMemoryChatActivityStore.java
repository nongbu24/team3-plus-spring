package com.example.team3plusspring.domain.chat.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Profile("!redis-chat")
public class InMemoryChatActivityStore implements ChatActivityStore {
    private final Map<Long, RoomActivity> roomActivities = new ConcurrentHashMap<>();

    @Override
    public void refreshRoomActivity(Long roomId) {
        roomActivities.computeIfAbsent(roomId, key -> new RoomActivity())
                .refresh();
    }

    @Override
    public Set<Long> findAndMarkWarningRoomIds(Set<Long> roomIds, Duration warningAfter) {
        LocalDateTime warningThreshold = LocalDateTime.now().minus(warningAfter);

        return roomIds.stream()
                .filter(roomId -> {
                    RoomActivity activity = roomActivities.get(roomId);
                    return activity != null && activity.needsWarning(warningThreshold);
                })
                .peek(roomId -> roomActivities.get(roomId).markWarningSent())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Long> findExpiredRoomIds(Set<Long> roomIds, Duration timeout) {
        LocalDateTime expiredThreshold = LocalDateTime.now().minus(timeout);

        return roomIds.stream()
                .filter(roomId -> {
                    RoomActivity activity = roomActivities.get(roomId);
                    return activity == null || activity.isExpired(expiredThreshold);
                })
                .collect(Collectors.toSet());
    }

    private static class RoomActivity {
        private LocalDateTime lastActivityAt;
        private boolean warningSent;

        private RoomActivity() {
            refresh();
        }

        private void refresh() {
            this.lastActivityAt = LocalDateTime.now();
            this.warningSent = false;
        }

        private boolean needsWarning(LocalDateTime threshold) {
            return !warningSent && !lastActivityAt.isAfter(threshold);
        }

        private boolean isExpired(LocalDateTime threshold) {
            return !lastActivityAt.isAfter(threshold);
        }

        private void markWarningSent() {
            this.warningSent = true;
        }
    }
}
