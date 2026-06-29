package com.example.team3plusspring.domain.chat.service;

import java.time.Duration;
import java.util.Set;

public interface ChatActivityStore {
    void refreshRoomActivity(Long roomId);

    void removeRoomActivity(Long roomId);

    Set<Long> findAndMarkWarningRoomIds(Set<Long> roomIds, Duration warningAfter);

    Set<Long> findExpiredRoomIds(Set<Long> roomIds, Duration timeout);
}
