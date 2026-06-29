package com.example.team3plusspring.domain.chat.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatAdminSessionServiceTest {

    private final ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
    private final WebSocketSessionStore webSocketSessionStore = mock(WebSocketSessionStore.class);
    private final ChatAdminAssignmentEventPublisher eventPublisher = mock(ChatAdminAssignmentEventPublisher.class);
    private final ChatAdminSessionService chatAdminSessionService = new ChatAdminSessionService(
            chatSessionRegistry,
            webSocketSessionStore,
            eventPublisher
    );

    @Test
    void 담당자가배정되면_현재서버의다른관리자세션을닫고_배정이벤트를발행한다() {
        // given
        when(chatSessionRegistry.removeAdminSessionsExcept(1L, 10L)).thenReturn(Set.of("session-1", "session-2"));

        // when
        chatAdminSessionService.handleAdminAssigned(1L, 10L);

        // then
        verify(webSocketSessionStore).close("session-1");
        verify(webSocketSessionStore).close("session-2");
        verify(eventPublisher).publish(1L, 10L);
    }

    @Test
    void 원격배정이벤트를받으면_현재서버의다른관리자세션만닫는다() {
        // given
        when(chatSessionRegistry.removeAdminSessionsExcept(1L, 10L)).thenReturn(Set.of("session-1"));

        // when
        chatAdminSessionService.closeOtherAdminSessions(1L, 10L);

        // then
        verify(webSocketSessionStore).close("session-1");
    }
}
