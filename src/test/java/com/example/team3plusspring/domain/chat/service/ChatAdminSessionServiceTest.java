package com.example.team3plusspring.domain.chat.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatAdminSessionServiceTest {

    private final ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
    private final ChatWebSocketSessionManager chatWebSocketSessionManager = mock(ChatWebSocketSessionManager.class);
    private final ChatAdminAssignmentEventPublisher eventPublisher = mock(ChatAdminAssignmentEventPublisher.class);
    private final ChatAdminSessionService chatAdminSessionService = new ChatAdminSessionService(
            chatSessionRegistry,
            chatWebSocketSessionManager,
            eventPublisher
    );

    @Test
    void 담당자가배정되면_현재서버의다른관리자구독을무효화하고_배정이벤트를발행한다() {
        // given
        when(chatSessionRegistry.removeAdminSessionsExcept(1L, 10L))
                .thenReturn(Set.of(new ChatSessionRegistry.RemovedAdminSubscription("session-1", "sub-1")));

        // when
        chatAdminSessionService.handleAdminAssigned(1L, 10L);

        // then
        verify(chatSessionRegistry).removeAdminSessionsExcept(1L, 10L);
        verify(chatWebSocketSessionManager).closeSessions(Set.of("session-1"));
        verify(eventPublisher).publish(1L, 10L);
    }

    @Test
    void 원격배정이벤트를받으면_현재서버의다른관리자구독만무효화한다() {
        // given
        when(chatSessionRegistry.removeAdminSessionsExcept(1L, 10L))
                .thenReturn(Set.of(new ChatSessionRegistry.RemovedAdminSubscription("session-1", "sub-1")));

        // when
        chatAdminSessionService.closeOtherAdminSessions(1L, 10L);

        // then
        verify(chatSessionRegistry).removeAdminSessionsExcept(1L, 10L);
        verify(chatWebSocketSessionManager).closeSessions(Set.of("session-1"));
    }
}
