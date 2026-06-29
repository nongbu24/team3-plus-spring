package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.user.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ChatSessionRegistry {
    private final ChatActivityStore chatActivityStore;

    /**
     * sessionId는 브라우저 탭마다 생기는 WebSocket 연결 식별자다.
     * 한 세션이 어떤 사용자/방 조합에 들어갔는지 기억해 disconnect 때 정리한다.
     */
    private final Map<String, Set<SessionRoom>> sessionRooms = new ConcurrentHashMap<>();

    // 실제 STOMP 구독 세션을 기억해 강제 퇴장이나 담당자 배정 시 기존 구독을 끊는다.
    private final Map<String, Set<SessionSubscription>> sessionSubscriptions = new ConcurrentHashMap<>();

    // 같은 사용자가 같은 방을 여러 탭으로 열 수 있으므로 userId + roomId 기준 활성 세션 수를 센다.
    private final Map<SessionRoom, Integer> activeSessionCounts = new ConcurrentHashMap<>();

    @Autowired
    public ChatSessionRegistry(ChatActivityStore chatActivityStore) {
        this.chatActivityStore = chatActivityStore;
    }

    ChatSessionRegistry() {
        this(new InMemoryChatActivityStore());
    }

    public synchronized void subscribe(String sessionId, Long userId, UserRole role, Long roomId) {
        SessionSubscription subscription = new SessionSubscription(userId, role, roomId);
        sessionSubscriptions.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(subscription);
    }

    public synchronized boolean isSubscribed(String sessionId, Long roomId) {
        Set<SessionSubscription> subscriptions = sessionSubscriptions.get(sessionId);

        return subscriptions != null && subscriptions.stream()
                .anyMatch(subscription -> subscription.roomId().equals(roomId));
    }

    public synchronized boolean isEntered(String sessionId, Long userId, Long roomId) {
        Set<SessionRoom> rooms = sessionRooms.get(sessionId);

        return rooms != null && rooms.contains(new SessionRoom(userId, roomId));
    }

    public synchronized boolean canSend(String sessionId, Long userId, Long roomId) {
        return isSubscribed(sessionId, roomId) && isEntered(sessionId, userId, roomId);
    }

    // 같은 세션에서 같은 방에 enter가 반복되어도 활성 세션 수는 한 번만 증가한다.
    public synchronized boolean enter(String sessionId, Long userId, Long roomId) {
        SessionRoom sessionRoom = new SessionRoom(userId, roomId);
        boolean added = sessionRooms.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(sessionRoom);

        if (added) {
            activeSessionCounts.merge(sessionRoom, 1, Integer::sum);
        }

        chatActivityStore.refreshRoomActivity(roomId);

        return added;
    }

    public synchronized void refreshRoomActivity(Long roomId) {
        chatActivityStore.refreshRoomActivity(roomId);
    }

    public synchronized Set<Long> findAndMarkWarningRoomIds(Duration warningAfter) {
        return chatActivityStore.findAndMarkWarningRoomIds(activeRoomIds(), warningAfter);
    }

    public synchronized Set<InactiveChatSession> expireInactiveSessions(Duration timeout) {
        Set<Long> expiredRoomIds = chatActivityStore.findExpiredRoomIds(activeRoomIds(), timeout);

        Set<SessionRoom> expiredRooms = activeSessionCounts.keySet()
                .stream()
                .filter(sessionRoom -> expiredRoomIds.contains(sessionRoom.roomId()))
                .collect(Collectors.toSet());

        return expiredRooms.stream()
                .map(this::expire)
                .collect(Collectors.toSet());
    }

    // 사용자가 직접 퇴장한 경우에는 같은 사용자/방 조합의 모든 세션을 정리한다.
    public synchronized void leaveAll(Long userId, Long roomId) {
        SessionRoom sessionRoom = new SessionRoom(userId, roomId);

        sessionRooms.forEach((sessionId, rooms) -> rooms.remove(sessionRoom));
        sessionRooms.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        sessionSubscriptions.forEach((sessionId, subscriptions) ->
                subscriptions.removeIf(subscription -> subscription.isSameUserRoom(userId, roomId))
        );

        sessionSubscriptions.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        activeSessionCounts.remove(sessionRoom);
    }

    public synchronized void removeAdminSessionsExcept(Long roomId, Long assignedAdminId) {
        sessionSubscriptions.forEach((sessionId, subscriptions) -> {
            boolean removed = subscriptions.removeIf(subscription -> subscription.isOtherAdmin(roomId, assignedAdminId));

            if (removed) {
                removeEnteredRoom(sessionId, roomId);
            }
        });

        sessionSubscriptions.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());
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
        }
    }

    private InactiveChatSession expire(SessionRoom sessionRoom) {
        leaveAll(sessionRoom.userId(), sessionRoom.roomId());

        return new InactiveChatSession(sessionRoom.userId(), sessionRoom.roomId());
    }

    private Set<Long> activeRoomIds() {
        return activeSessionCounts.keySet()
                .stream()
                .map(SessionRoom::roomId)
                .collect(Collectors.toSet());
    }

    public record InactiveChatSession(Long userId, Long roomId) {
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
