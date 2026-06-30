package com.example.team3plusspring.domain.chat.port;

import java.time.Duration;
import java.util.Set;

public interface ChatActivityStore {
    void refreshRoomActivity(Long roomId);

    void removeRoomActivity(Long roomId);

    default boolean preservesSharedRoomActivity() {
        return false;
    }

    Set<Long> findAndMarkWarningRoomIds(Set<Long> roomIds, Duration warningAfter);

    Set<ActiveChatSession> findAndClaimExpiredSessions(Set<ActiveChatSession> activeSessions, Duration timeout);

    default boolean isExpiredClaimStillValid(ActiveChatSession activeSession) {
        return true;
    }
}
