package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.local.InMemoryChatActivityStore;
import com.example.team3plusspring.domain.chat.port.ActiveChatSession;
import com.example.team3plusspring.domain.chat.port.ChatActivityStore;
import com.example.team3plusspring.domain.chat.port.InactiveChatSession;
import com.example.team3plusspring.domain.chat.port.RemovedSubscription;
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

    public synchronized void subscribe(String sessionId, String subscriptionId, Long userId, UserRole role, Long roomId) {
        SessionSubscription subscription = new SessionSubscription(subscriptionId, userId, role, roomId);
        sessionSubscriptions.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(subscription);
    }

    public synchronized boolean isSubscribed(String sessionId, Long roomId) {
        Set<SessionSubscription> subscriptions = sessionSubscriptions.get(sessionId);

        return subscriptions != null && subscriptions.stream()
                .anyMatch(subscription -> subscription.getRoomId().equals(roomId));
    }

    public synchronized void unsubscribe(String sessionId, String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }

        Set<SessionSubscription> subscriptions = sessionSubscriptions.get(sessionId);

        if (subscriptions == null) {
            return;
        }

        subscriptions.removeIf(subscription -> subscription.isSameSubscription(subscriptionId));

        if (subscriptions.isEmpty()) {
            sessionSubscriptions.remove(sessionId);
        }
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

    public synchronized void rollbackEnter(String sessionId, Long userId, Long roomId) {
        Set<SessionRoom> rooms = sessionRooms.get(sessionId);

        if (rooms == null) {
            return;
        }

        SessionRoom sessionRoom = new SessionRoom(userId, roomId);

        if (!rooms.remove(sessionRoom)) {
            return;
        }

        if (rooms.isEmpty()) {
            sessionRooms.remove(sessionId);
        }

        decreaseActiveSessionCount(sessionRoom);
    }

    public synchronized void refreshRoomActivity(Long roomId) {
        chatActivityStore.refreshRoomActivity(roomId);
    }

    public synchronized Set<Long> findAndMarkWarningRoomIds(Duration warningAfter) {
        return chatActivityStore.findAndMarkWarningRoomIds(activeRoomIds(), warningAfter);
    }

    public synchronized Set<InactiveChatSession> expireInactiveSessions(Duration timeout) {
        return chatActivityStore.findAndClaimExpiredSessions(activeSessions(), timeout)
                .stream()
                .filter(chatActivityStore::isExpiredClaimStillValid)
                .map(activeSession -> expire(new SessionRoom(activeSession.getUserId(), activeSession.getRoomId())))
                .collect(Collectors.toSet());
    }

    public synchronized Set<RemovedSubscription> removeLocalSessions(Long userId, Long roomId) {
        SessionRoom sessionRoom = new SessionRoom(userId, roomId);
        Set<RemovedSubscription> removedSubscriptions = new HashSet<>();

        sessionRooms.forEach((sessionId, rooms) -> rooms.remove(sessionRoom));
        sessionRooms.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        sessionSubscriptions.forEach((sessionId, subscriptions) -> {
            Set<SessionSubscription> targetSubscriptions = subscriptions.stream()
                    .filter(subscription -> subscription.isSameUserRoom(userId, roomId))
                    .collect(Collectors.toSet());

            targetSubscriptions.forEach(subscription ->
                    removedSubscriptions.add(new RemovedSubscription(sessionId, subscription.getSubscriptionId()))
            );
            subscriptions.removeAll(targetSubscriptions);
        });

        sessionSubscriptions.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        activeSessionCounts.remove(sessionRoom);
        removeRoomActivityIfNoActiveSession(roomId);

        return removedSubscriptions;
    }

    public synchronized Set<RemovedSubscription> removeAdminSessionsExcept(Long roomId, Long assignedAdminId) {
        Set<RemovedSubscription> removedSubscriptions = new HashSet<>();

        sessionSubscriptions.forEach((sessionId, subscriptions) -> {
            Set<SessionSubscription> targetSubscriptions = subscriptions.stream()
                    .filter(subscription -> subscription.isOtherAdmin(roomId, assignedAdminId))
                    .collect(Collectors.toSet());

            if (!targetSubscriptions.isEmpty()) {
                targetSubscriptions.forEach(subscription ->
                        removedSubscriptions.add(new RemovedSubscription(sessionId, subscription.getSubscriptionId()))
                );
                subscriptions.removeAll(targetSubscriptions);
                removeEnteredRoom(sessionId, roomId);
            }
        });

        sessionSubscriptions.entrySet()
                .removeIf(entry -> entry.getValue().isEmpty());

        return removedSubscriptions;
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
            decreaseActiveSessionCount(room);
        }
    }

    private void removeEnteredRoom(String sessionId, Long roomId) {
        Set<SessionRoom> rooms = sessionRooms.get(sessionId);

        if (rooms == null) {
            return;
        }

        Set<SessionRoom> targetRooms = new HashSet<>(rooms);
        targetRooms.removeIf(room -> !room.getRoomId().equals(roomId));

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
            removeRoomActivityIfNoActiveSession(room.getRoomId());
        }
    }

    private void removeRoomActivityIfNoActiveSession(Long roomId) {
        if (hasActiveSession(roomId)) {
            return;
        }

        if (chatActivityStore.preservesSharedRoomActivity()) {
            return;
        }

        chatActivityStore.removeRoomActivity(roomId);
    }

    private boolean hasActiveSession(Long roomId) {
        return activeSessionCounts.keySet()
                .stream()
                .anyMatch(room -> room.getRoomId().equals(roomId));
    }

    private InactiveChatSession expire(SessionRoom sessionRoom) {
        Set<RemovedSubscription> removedSubscriptions =
                removeLocalSessions(sessionRoom.getUserId(), sessionRoom.getRoomId());

        return new InactiveChatSession(sessionRoom.getUserId(), sessionRoom.getRoomId(), removedSubscriptions);
    }

    private Set<Long> activeRoomIds() {
        return activeSessionCounts.keySet()
                .stream()
                .map(SessionRoom::getRoomId)
                .collect(Collectors.toSet());
    }

    private Set<ActiveChatSession> activeSessions() {
        return activeSessionCounts.keySet()
                .stream()
                .map(sessionRoom -> new ActiveChatSession(sessionRoom.getUserId(), sessionRoom.getRoomId()))
                .collect(Collectors.toSet());
    }

}
