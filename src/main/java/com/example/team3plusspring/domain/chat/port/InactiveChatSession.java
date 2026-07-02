package com.example.team3plusspring.domain.chat.port;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;

@Getter
@EqualsAndHashCode
public class InactiveChatSession {
    private final Long userId;
    private final Long roomId;
    private final Set<RemovedSubscription> removedSubscriptions;

    public InactiveChatSession(Long userId, Long roomId) {
        this(userId, roomId, Set.of());
    }

    public InactiveChatSession(Long userId, Long roomId, Set<RemovedSubscription> removedSubscriptions) {
        this.userId = userId;
        this.roomId = roomId;
        this.removedSubscriptions = removedSubscriptions;
    }
}
