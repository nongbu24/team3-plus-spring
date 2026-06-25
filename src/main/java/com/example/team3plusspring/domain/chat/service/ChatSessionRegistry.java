package com.example.team3plusspring.domain.chat.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionRegistry {
    private final ConcurrentHashMap<String, Set<Long>> sessionRoomIds = new ConcurrentHashMap<>();

    public void enter(String sessionId, Long roomId) {
        sessionRoomIds.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(roomId);
    }

    public void leave(String sessionId, Long roomId) {
        Set<Long> roomIds = sessionRoomIds.get(sessionId);
        if (roomIds == null) {
            return;
        }

        roomIds.remove(roomId);
        if (roomIds.isEmpty()) {
            sessionRoomIds.remove(sessionId);
        }
    }

    public Set<Long> removeSession(String sessionId) {
        Set<Long> roomIds = sessionRoomIds.remove(sessionId);
        if (roomIds == null) {
            return Collections.emptySet();
        }

        return Set.copyOf(roomIds);
    }
}
