package com.example.team3plusspring.domain.chat.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionRegistry {
    // sessionId는 브라우저 탭마다 생기는 WebSocket 연결 식별자다.
    // 한 세션이 어떤 사용자/방 조합에 들어갔는지 기억해 disconnect 때 정리한다.
    private final Map<String, Set<SessionRoom>> sessionRooms = new ConcurrentHashMap<>();

    // 같은 사용자가 같은 방을 여러 탭으로 열 수 있으므로 userId + roomId 기준 활성 세션 수를 센다.
    private final Map<SessionRoom, Integer> activeSessionCounts = new ConcurrentHashMap<>();

    // 같은 세션에서 같은 방에 enter가 반복되어도 활성 세션 수는 한 번만 증가한다.
    public synchronized void enter(String sessionId, Long userId, Long roomId) {
        SessionRoom sessionRoom = new SessionRoom(userId, roomId);
        boolean added = sessionRooms.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(sessionRoom);

        if (added) {
            activeSessionCounts.merge(sessionRoom, 1, Integer::sum);
        }
    }

    // 사용자가 직접 퇴장한 경우에는 같은 사용자/방 조합의 모든 세션을 정리한다.
    public synchronized void leave(String sessionId, Long userId, Long roomId) {
        SessionRoom sessionRoom = new SessionRoom(userId, roomId);

        sessionRooms.values()
                .forEach(rooms -> rooms.remove(sessionRoom));
        sessionRooms.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        activeSessionCounts.remove(sessionRoom);
    }

    // 끊긴 세션을 제거하고, 그 결과 마지막 연결까지 사라진 방 id만 반환한다.
    public synchronized Set<Long> removeSession(String sessionId) {
        Set<SessionRoom> rooms = sessionRooms.remove(sessionId);

        if (rooms == null) {
            return Collections.emptySet();
        }

        Set<Long> roomsToLeave = new HashSet<>();

        for (SessionRoom room : rooms) {
            int remainingCount = activeSessionCounts.merge(room, -1, Integer::sum);

            if (remainingCount <= 0) {
                activeSessionCounts.remove(room);
                roomsToLeave.add(room.roomId());
            }
        }

        return roomsToLeave;
    }

    private record SessionRoom(Long userId, Long roomId) {
    }
}
