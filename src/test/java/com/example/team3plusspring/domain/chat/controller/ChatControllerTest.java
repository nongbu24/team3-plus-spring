package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatMessageRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.dto.ChatRoomEventRequest;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import com.example.team3plusspring.domain.chat.port.ChatMessagePublisher;
import com.example.team3plusspring.domain.chat.port.ChatSessionExpiredEventPublisher;
import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    ChatSessionExpiredEventPublisher chatSessionExpiredEventPublisher;

    @InjectMocks
    ChatController chatController;

    @Test
    void 메시지전송_인증사용자이면_메시지를저장하고발행한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatMessageRequest request = ChatMessageRequest.of(1L, "안녕하세요");
        ChatMessageResponse response = response(10L, "안녕하세요");

        when(chatSessionRegistry.canSend("session-1", user.getId(), 1L)).thenReturn(true);
        when(chatFacade.sendMessage(request, user)).thenReturn(response);

        // when
        chatController.sendMessage(request, "session-1", authentication);

        // then
        verify(chatSessionRegistry).canSend("session-1", user.getId(), 1L);
        verify(chatFacade).sendMessage(request, user);
        verify(chatSessionRegistry).refreshRoomActivity(1L);
        verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 메시지전송_관리자가처음응답해서담당자가되어도_컨트롤러는메시지만발행한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatMessageRequest request = ChatMessageRequest.of(1L, "확인해보겠습니다");
        ChatMessageResponse response = response(10L, "확인해보겠습니다");

        when(chatSessionRegistry.canSend("session-1", user.getId(), 1L)).thenReturn(true);
        when(chatFacade.sendMessage(request, user)).thenReturn(response);

        // when
        chatController.sendMessage(request, "session-1", authentication);

        // then
        verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 입장_인증사용자이면_세션을등록하고입장메시지를발행한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = ChatRoomEventRequest.of(1L);
        ChatMessageResponse response = response(11L, "홍길동님이 입장했습니다");

        when(chatSessionRegistry.isSubscribed("session-1", 1L)).thenReturn(true);
        when(chatFacade.enterRoom(1L, user)).thenReturn(response);
        when(chatSessionRegistry.enter("session-1", user.getId(), 1L)).thenReturn(true);

        // when
        chatController.enterRoom(request, "session-1", authentication);

        // then
        var inOrder = inOrder(chatSessionRegistry, chatFacade, chatMessagePublisher);
        inOrder.verify(chatSessionRegistry).isSubscribed("session-1", 1L);
        inOrder.verify(chatSessionRegistry).enter("session-1", user.getId(), 1L);
        inOrder.verify(chatFacade).enterRoom(1L, user);
        inOrder.verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 입장_현재세션이방을구독한상태가아니면_실패한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = ChatRoomEventRequest.of(1L);

        when(chatSessionRegistry.isSubscribed("session-1", 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> chatController.enterRoom(request, "session-1", authentication))
                .isInstanceOf(BusinessException.class);
        verify(chatSessionRegistry).isSubscribed("session-1", 1L);
        verify(chatSessionRegistry, never()).enter("session-1", user.getId(), 1L);
        verifyNoInteractions(chatFacade, chatMessagePublisher);
    }

    @Test
    void 입장_이미선점된세션이면_입장메시지를저장하거나발행하지않는다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = ChatRoomEventRequest.of(1L);

        when(chatSessionRegistry.isSubscribed("session-1", 1L)).thenReturn(true);
        when(chatSessionRegistry.enter("session-1", user.getId(), 1L)).thenReturn(false);

        // when
        chatController.enterRoom(request, "session-1", authentication);

        // then
        verify(chatSessionRegistry).isSubscribed("session-1", 1L);
        verify(chatSessionRegistry).enter("session-1", user.getId(), 1L);
        verifyNoInteractions(chatFacade, chatMessagePublisher);
    }

    @Test
    void 입장_DB처리에실패하면_선점한입장상태를되돌린다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = ChatRoomEventRequest.of(1L);

        when(chatSessionRegistry.isSubscribed("session-1", 1L)).thenReturn(true);
        when(chatSessionRegistry.enter("session-1", user.getId(), 1L)).thenReturn(true);
        when(chatFacade.enterRoom(1L, user)).thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED));

        // when & then
        assertThatThrownBy(() -> chatController.enterRoom(request, "session-1", authentication))
                .isInstanceOf(BusinessException.class);

        var inOrder = inOrder(chatSessionRegistry, chatFacade);
        inOrder.verify(chatSessionRegistry).isSubscribed("session-1", 1L);
        inOrder.verify(chatSessionRegistry).enter("session-1", user.getId(), 1L);
        inOrder.verify(chatFacade).enterRoom(1L, user);
        inOrder.verify(chatSessionRegistry).rollbackEnter("session-1", user.getId(), 1L);
        verifyNoInteractions(chatMessagePublisher);
    }

    @Test
    void 메시지전송_현재세션이방에입장하고구독한상태가아니면_실패한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatMessageRequest request = ChatMessageRequest.of(1L, "안녕하세요");

        when(chatSessionRegistry.canSend("session-1", user.getId(), 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> chatController.sendMessage(request, "session-1", authentication))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(chatFacade, chatMessagePublisher);
    }

    @Test
    void 명시적퇴장_같은사용자의같은방구독을무효화한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = ChatRoomEventRequest.of(1L);
        ChatMessageResponse response = response(12L, "홍길동님이 퇴장했습니다");

        when(chatSessionRegistry.isEntered("session-1", user.getId(), 1L)).thenReturn(true);
        when(chatFacade.leaveRoom(1L, user)).thenReturn(response);

        // when
        chatController.leaveRoom(request, "session-1", authentication);

        // then
        verify(chatSessionRegistry).isEntered("session-1", user.getId(), 1L);
        verify(chatFacade).leaveRoom(1L, user);
        verify(chatSessionRegistry).removeLocalSessions(user.getId(), 1L);
        verify(chatSessionExpiredEventPublisher).publish(1L, user.getId());
        verify(chatMessagePublisher).publish(1L, response);
    }

    @Test
    void 명시적퇴장_이미완료된방이면_메시지는발행하지않고_세션만정리한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = ChatRoomEventRequest.of(1L);

        when(chatSessionRegistry.isEntered("session-1", user.getId(), 1L)).thenReturn(true);
        when(chatFacade.leaveRoom(1L, user)).thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_COMPLETED));

        // when
        chatController.leaveRoom(request, "session-1", authentication);

        // then
        verify(chatSessionRegistry).isEntered("session-1", user.getId(), 1L);
        verify(chatFacade).leaveRoom(1L, user);
        verify(chatSessionRegistry).removeLocalSessions(user.getId(), 1L);
        verify(chatSessionExpiredEventPublisher).publish(1L, user.getId());
        verifyNoInteractions(chatMessagePublisher);
    }

    @Test
    void 명시적퇴장_현재세션이방에입장한상태가아니면_실패한다() {
        // given
        Authentication authentication = authentication();
        User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
        ChatRoomEventRequest request = ChatRoomEventRequest.of(1L);

        when(chatSessionRegistry.isEntered("session-1", user.getId(), 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> chatController.leaveRoom(request, "session-1", authentication))
                .isInstanceOf(BusinessException.class);
        verify(chatSessionRegistry).isEntered("session-1", user.getId(), 1L);
        verifyNoInteractions(chatFacade, chatMessagePublisher);
    }

    @Test
    void 연결종료_입장한방이있어도_세션만정리하고_퇴장처리는하지않는다() {
        // given
        Authentication authentication = authentication();
        org.springframework.web.socket.messaging.SessionDisconnectEvent event =
                new org.springframework.web.socket.messaging.SessionDisconnectEvent(
                        this,
                        org.springframework.messaging.support.MessageBuilder.withPayload(new byte[0]).build(),
                        "session-1",
                        org.springframework.web.socket.CloseStatus.NORMAL,
                        authentication
                );

        // when
        chatController.handleDisconnect(event);

        // then
        verify(chatSessionRegistry).removeSession("session-1");
        verifyNoInteractions(chatFacade, chatMessagePublisher);
    }

    @Test
    void 메시지전송_인증정보가없으면_실패한다() {
        // given
        ChatMessageRequest request = ChatMessageRequest.of(1L, "안녕하세요");

        // when & then
        assertThatThrownBy(() -> chatController.sendMessage(request, "session-1", null))
                .isInstanceOf(BusinessException.class);
    }

    private Authentication authentication() {
        User user = User.create("user@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(user, "id", 1L);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private ChatMessageResponse response(Long messageId, String content) {
        return ChatMessageResponse.of(
                messageId,
                content,
                1L,
                "홍길동",
                LocalDateTime.of(2026, 6, 25, 10, 30)
        );
    }
}
