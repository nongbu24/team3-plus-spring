package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.port.ChatAdminAssignmentEventPublisher;
import com.example.team3plusspring.domain.chat.port.RemovedSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatAdminSessionService {
    private final ChatSessionRegistry chatSessionRegistry;
    private final ChatStompSubscriptionManager chatStompSubscriptionManager;
    private final ChatAdminAssignmentEventPublisher chatAdminAssignmentEventPublisher;

    /**
     * 담당자가 새로 배정되면 현재 서버의 다른 관리자 구독을 먼저 무효화하고,
     * Redis 모드에서는 다른 서버들도 자기 로컬 세션을 정리할 수 있게 이벤트를 발행한다.
     */
    public void handleAdminAssigned(Long roomId, Long assignedAdminId) {
        closeOtherAdminSessions(roomId, assignedAdminId);
        chatAdminAssignmentEventPublisher.publish(roomId, assignedAdminId);
    }

    /**
     * Redis에서 배정 이벤트를 받은 서버는 다시 Redis로 발행하면 안 된다.
     * 그래서 원격 이벤트 수신자는 이 메서드만 호출해서 자기 서버의 구독만 무효화한다.
     */
    public void closeOtherAdminSessions(Long roomId, Long assignedAdminId) {
        Set<RemovedSubscription> removedSubscriptions =
                chatSessionRegistry.removeAdminSessionsExcept(roomId, assignedAdminId);

        chatStompSubscriptionManager.unsubscribeAll(removedSubscriptions);
    }
}
