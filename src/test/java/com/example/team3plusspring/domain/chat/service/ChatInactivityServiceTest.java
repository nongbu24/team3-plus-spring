package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatInactivityServiceTest {

    @Mock
    ChatSessionRegistry chatSessionRegistry;

    @Mock
    ChatFacade chatFacade;

    @Mock
    ChatMessagePublisher chatMessagePublisher;

    @InjectMocks
    ChatInactivityService chatInactivityService;

    @Test
    void 무활동경고대상이있으면_경고메시지를발행한다() {
        // given
        when(chatSessionRegistry.findAndMarkWarningRoomIds(Duration.ofMinutes(4).plusSeconds(30)))
                .thenReturn(Set.of(1L));

        // when
        chatInactivityService.closeInactiveSessions();

        // then
        verify(chatMessagePublisher).publish(
                eq(1L),
                argThat(message -> "일정 시간 동안 채팅 입력이 없다면 자동으로 채팅이 종료됩니다.".equals(message.getContent()))
        );
    }

    @Test
    void 무활동만료대상이있으면_퇴장처리하고_퇴장메시지를발행한다() {
        // given
        ChatMessageResponse response = new ChatMessageResponse(
                10L,
                "홍길동님이 퇴장했습니다",
                1L,
                "홍길동",
                LocalDateTime.of(2026, 6, 25, 10, 30)
        );

        when(chatSessionRegistry.expireInactiveSessions(Duration.ofMinutes(5)))
                .thenReturn(Set.of(new ChatSessionRegistry.InactiveChatSession(1L, 10L)));
        when(chatFacade.leaveInactiveRoom(10L, 1L)).thenReturn(Optional.of(response));

        // when
        chatInactivityService.closeInactiveSessions();

        // then
        verify(chatFacade).leaveInactiveRoom(10L, 1L);
        verify(chatMessagePublisher).publish(10L, response);
    }
}
