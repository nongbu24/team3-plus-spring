package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.user.entity.UserRole;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChatSessionRegistry {
    /**
     * sessionId는 브라우저 탭마다 생기는 WebSocket 연결 식별자다.
     * 한 세션이 어떤 사용자/방 조합에 들어갔는지 기억해 disconnect 때 정리한다.
     */
    private final Map<String, Set<SessionRoom>> sessionRooms = new ConcurrentHashMap<>();

    // 실제 STOMP 구독 세션을 기억해 강제 퇴장이나 담당자 배정 시 기존 구독을 끊는다.
    private final Map<String, Set<SessionSubscription>> sessionSubscriptions = new ConcurrentHashMap<>();

    // 같은 사용자가 같은 방을 여러 탭으로 열 수 있으므로 userId + roomId 기준 활성 세션 수를 센다.
    private final Map<SessionRoom, Integer> activeSessionCounts = new ConcurrentHashMap<>();

    // userId + roomId 기준 마지막 채팅 활동 시각을 기억해 무활동 자동 종료를 판단한다.
    private final Map<SessionRoom, SessionActivity> sessionActivities = new ConcurrentHashMap<>();

    public synchronized void subscribe(String sessionId, Long userId, UserRole role, Long roomId) {
        SessionSubscription subscription = new SessionSubscription(userId, role, roomId);
        sessionSubscriptions.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(subscription);
    }

    // 같은 세션에서 같은 방에 enter가 반복되어도 활성 세션 수는 한 번만 증가한다.
    public synchronized boolean enter(String sessionId, Long userId, Long roomId) {
        SessionRoom sessionRoom = new SessionRoom(userId, roomId);
        boolean added = sessionRooms.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(sessionRoom);

        if (added) {
            activeSessionCounts.merge(sessionRoom, 1, Integer::sum);
        }

        refreshActivity(sessionRoom);

        return added;
    }

    public synchronized void refreshRoomActivity(Long roomId) {
        sessionActivities.entrySet()
                .stream()
                .filter(entry -> entry.getKey().roomId().equals(roomId))
                .map(Map.Entry::getValue)
                .forEach(SessionActivity::refresh);
    }

    public synchronized Set<Long> findAndMarkWarningRoomIds(Duration warningAfter) {
        LocalDateTime warningThreshold = LocalDateTime.now().minus(warningAfter);

        return sessionActivities.entrySet()
                .stream()
                .filter(entry -> entry.getValue().needsWarning(warningThreshold))
                .peek(entry -> entry.getValue().markWarningSent())
                .map(entry -> entry.getKey().roomId())
                .collect(Collectors.toSet());
    }

    public synchronized Set<InactiveChatSession> expireInactiveSessions(Duration timeout) {
        LocalDateTime expiredThreshold = LocalDateTime.now().minus(timeout);
        Set<SessionRoom> expiredRooms = sessionActivities.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isExpired(expiredThreshold))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        return expiredRooms.stream()
                .map(this::expire)
                .collect(Collectors.toSet());
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
        sessionActivities.remove(sessionRoom);

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

    public synchronized void removeSession(String sessionId) {
        sessionSubscriptions.remove(sessionId);
        removeSessionRooms(sessionId);
    }

    private void removeSessionRooms(String sessionId) {
        Set<SessionRoom> rooms = sessionRooms.remove(sessionId);

        if (rooms == null) {
            return;
        }

        for (SessionRoom room : rooms) {
            int remainingCount = activeSessionCounts.merge(room, -1, Integer::sum);

            if (remainingCount <= 0) {
                activeSessionCounts.remove(room);
                sessionActivities.remove(room);
            }
        }
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
            sessionActivities.remove(room);
        }
    }

    private void refreshActivity(SessionRoom sessionRoom) {
        sessionActivities.computeIfAbsent(sessionRoom, key -> new SessionActivity())
                .refresh();
    }

    private InactiveChatSession expire(SessionRoom sessionRoom) {
        Set<String> sessionIds = leaveAll(sessionRoom.userId(), sessionRoom.roomId());

        return new InactiveChatSession(sessionRoom.userId(), sessionRoom.roomId(), sessionIds);
    }

    public record InactiveChatSession(Long userId, Long roomId, Set<String> sessionIds) {
    }

    private static class SessionActivity {
        private LocalDateTime lastActivityAt;
        private boolean warningSent;

        private SessionActivity() {
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
