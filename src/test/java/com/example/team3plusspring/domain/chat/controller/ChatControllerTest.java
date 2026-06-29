package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatMessageRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.dto.ChatRoomEventRequest;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import com.example.team3plusspring.domain.chat.facade.ChatSendResult;
import com.example.team3plusspring.domain.chat.service.ChatMessagePublisher;
import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import com.example.team3plusspring.domain.chat.service.WebSocketSessionStore;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    ChatFacade chatFacade;

    @Mock
    ChatMessagePublisher chatMessagePublisher;

    @Mock
    ChatSessionRegistry chatSessionRegistry;

    @Mock
    WebSocketSessionStore webSocketSessionStore;

    @InjectMocks
    ChatController chatController;

    @Test
    void 메시지전송_인증사용자이면_메시지를저장하고발행한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatMessageRequest request = new ChatMessageRequest(1L, "안녕하세요");
        ChatMessageResponse response = response(10L, "안녕하세요");

        when(chatFacade.sendMessage(request, user)).thenReturn(new ChatSendResult(response, null));

        // when
        chatController.send(request, authentication);

        // then
        verify(chatFacade).sendMessage(request, user);
        verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 메시지전송_관리자가처음응답해서담당자가되어도_컨트롤러는메시지만발행한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatMessageRequest request = new ChatMessageRequest(1L, "확인해보겠습니다");
        ChatMessageResponse response = response(10L, "확인해보겠습니다");

        when(chatFacade.sendMessage(request, user)).thenReturn(new ChatSendResult(response, user.getId()));

        // when
        chatController.send(request, authentication);

        // then
        verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 입장_인증사용자이면_세션을등록하고입장메시지를발행한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = new ChatRoomEventRequest(1L);
        ChatMessageResponse response = response(11L, "홍길동님이 입장했습니다");

        when(chatFacade.enterRoom(1L, user)).thenReturn(response);

        // when
        chatController.enter(request, "session-1", authentication);

        // then
        verify(chatFacade).enterRoom(1L, user);
        verify(chatSessionRegistry).enter("session-1", user.getId(), 1L);
        verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 명시적퇴장_같은사용자의같은방세션을모두닫는다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = new ChatRoomEventRequest(1L);
        ChatMessageResponse response = response(12L, "홍길동님이 퇴장했습니다");

        when(chatFacade.leaveRoom(1L, user)).thenReturn(response);
        when(chatSessionRegistry.leaveAll(user.getId(), 1L)).thenReturn(Set.of("session-1", "session-2"));

        // when
        chatController.leave(request, "session-1", authentication);

        // then
        verify(chatFacade).leaveRoom(1L, user);
        verify(chatSessionRegistry).leaveAll(user.getId(), 1L);
        verify(chatMessagePublisher).publish(1L, response);
        verify(webSocketSessionStore).close("session-1");
        verify(webSocketSessionStore).close("session-2");
    }

    @Test
    void 연결종료_입장한방이있으면_퇴장메시지를발행한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatMessageResponse response = response(12L, "홍길동님이 퇴장했습니다");
        org.springframework.web.socket.messaging.SessionDisconnectEvent event =
                new org.springframework.web.socket.messaging.SessionDisconnectEvent(
                        this,
                        org.springframework.messaging.support.MessageBuilder.withPayload(new byte[0]).build(),
                        "session-1",
                        org.springframework.web.socket.CloseStatus.NORMAL,
                        authentication
                );

        when(chatSessionRegistry.removeSession("session-1")).thenReturn(Set.of(1L));
        when(chatFacade.leaveRoom(1L, user)).thenReturn(response);

        // when
        chatController.handleDisconnect(event);

        // then
        verify(chatSessionRegistry).removeSession("session-1");
        verify(chatFacade).leaveRoom(1L, user);
        verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 메시지전송_인증정보가없으면_실패한다() {
        // given
        ChatMessageRequest request = new ChatMessageRequest(1L, "안녕하세요");

        // when & then
        assertThatThrownBy(() -> chatController.send(request, null))
                .isInstanceOf(BusinessException.class);
    }

    private Authentication authentication() {
        User user = User.create("user@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(user, "id", 1L);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private ChatMessageResponse response(Long messageId, String content) {
        return new ChatMessageResponse(
                messageId,
                content,
                1L,
                "홍길동",
                LocalDateTime.of(2026, 6, 25, 10, 30)
        );
    }
}
