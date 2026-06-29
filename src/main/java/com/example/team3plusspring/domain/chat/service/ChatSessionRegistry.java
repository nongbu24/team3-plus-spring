package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.user.entity.UserRole;
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

    // 실제 STOMP 구독 세션을 기억해 강제 퇴장이나 담당자 배정 시 기존 구독을 끊는다.
    private final Map<String, Set<SessionSubscription>> sessionSubscriptions = new ConcurrentHashMap<>();

    // 같은 사용자가 같은 방을 여러 탭으로 열 수 있으므로 userId + roomId 기준 활성 세션 수를 센다.
    private final Map<SessionRoom, Integer> activeSessionCounts = new ConcurrentHashMap<>();

    public synchronized void subscribe(String sessionId, Long userId, UserRole role, Long roomId) {
        SessionSubscription subscription = new SessionSubscription(userId, role, roomId);
        sessionSubscriptions.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(subscription);
    }

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
    public synchronized Set<String> leaveAll(Long userId, Long roomId) {
        SessionRoom sessionRoom = new SessionRoom(userId, roomId);
        Set<String> sessionIds = new HashSet<>();

        sessionRooms.forEach((sessionId, rooms) -> {
            if (rooms.remove(sessionRoom)) {
                sessionIds.add(sessionId);
            }
        });
        sessionRooms.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        sessionSubscriptions.forEach((sessionId, subscriptions) -> {
            if (subscriptions.removeIf(subscription -> subscription.isSameUserRoom(userId, roomId))) {
                sessionIds.add(sessionId);
            }
        });
        sessionSubscriptions.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        activeSessionCounts.remove(sessionRoom);

        return sessionIds;
    }

    public synchronized Set<String> removeAdminSessionsExcept(Long roomId, Long assignedAdminId) {
        Set<String> sessionIds = new HashSet<>();

        sessionSubscriptions.forEach((sessionId, subscriptions) -> {
            boolean removed = subscriptions.removeIf(subscription -> subscription.isOtherAdmin(roomId, assignedAdminId));

            if (removed) {
                sessionIds.add(sessionId);
            }
        });
        sessionSubscriptions.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        sessionIds.forEach(sessionId -> removeEnteredRoom(sessionId, roomId));

        return sessionIds;
    }

    // 끊긴 세션을 제거하고, 그 결과 마지막 연결까지 사라진 방 id만 반환한다.
    public synchronized Set<Long> removeSession(String sessionId) {
        sessionSubscriptions.remove(sessionId);

        return removeSessionRooms(sessionId);
    }

    private Set<Long> removeSessionRooms(String sessionId) {
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

    private void removeEnteredRoom(String sessionId, Long roomId) {
        Set<SessionRoom> rooms = sessionRooms.get(sessionId);

        if (rooms == null) {
            return;
        }

        Set<SessionRoom> targetRooms = new HashSet<>(rooms);
        targetRooms.removeIf(room -> !room.roomId().equals(roomId));

        for (SessionRoom room : targetRooms) {
            rooms.remove(room);
            decreaseActiveSessionCount(room);
        }

        if (rooms.isEmpty()) {
            sessionRooms.remove(sessionId);
        }
    }

    private void decreaseActiveSessionCount(SessionRoom room) {
        int remainingCount = activeSessionCounts.merge(room, -1, Integer::sum);

        if (remainingCount <= 0) {
            activeSessionCounts.remove(room);
        }
    }

    private record SessionRoom(Long userId, Long roomId) {
    }

    private record SessionSubscription(Long userId, UserRole role, Long roomId) {
        private boolean isSameUserRoom(Long targetUserId, Long targetRoomId) {
            return userId.equals(targetUserId) && roomId.equals(targetRoomId);
        }

        private boolean isOtherAdmin(Long targetRoomId, Long assignedAdminId) {
            return role == UserRole.ADMIN && roomId.equals(targetRoomId) && !userId.equals(assignedAdminId);
        }
    }
}
