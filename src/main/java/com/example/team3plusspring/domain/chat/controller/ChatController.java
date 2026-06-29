package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatRoomEventRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import com.example.team3plusspring.domain.chat.facade.ChatSendResult;
import com.example.team3plusspring.domain.chat.service.ChatMessagePublisher;
import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import com.example.team3plusspring.domain.chat.service.WebSocketSessionStore;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatFacade chatFacade;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ChatSessionRegistry chatSessionRegistry;
    private final WebSocketSessionStore webSocketSessionStore;

    // 클라이언트가 /pub/chat.enter 로 보낸 입장 이벤트를 처리하고, /sub/chat/{roomId} 구독자에게 알린다.
    @MessageMapping("/chat.enter")
    public void enter(
            @Payload @Valid ChatRoomEventRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        User sender = getAuthenticatedUser(principal);
        boolean entered = chatSessionRegistry.enter(sessionId, sender.getId(), request.getRoomId());

        if (!entered) {
            return;
        }

        ChatMessageResponse response = chatFacade.enterRoom(request.getRoomId(), sender);
        chatMessagePublisher.publish(request.getRoomId(), response);
    }

    // 일반 채팅 메시지를 저장한 뒤 같은 방을 구독 중인 클라이언트들에게 발행한다.
    @MessageMapping("/chat.send")
    public void send(@Payload @Valid ChatMessageRequest request, Principal principal) {
        User sender = getAuthenticatedUser(principal);
        ChatSendResult result = chatFacade.sendMessage(request, sender);

        chatSessionRegistry.refreshRoomActivity(request.getRoomId());
        chatMessagePublisher.publish(request.getRoomId(), result.getMessage());
    }

    // 사용자가 직접 퇴장 버튼을 누른 경우다. 자동 연결 종료와 달리 명시적 퇴장으로 보고 바로 퇴장 메시지를 발행한다.
    @MessageMapping("/chat.leave")
    public void leave(
            @Payload @Valid ChatRoomEventRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        User sender = getAuthenticatedUser(principal);
        ChatMessageResponse response = chatFacade.leaveRoom(request.getRoomId(), sender);
        Set<String> sessionIds = chatSessionRegistry.leaveAll(sender.getId(), request.getRoomId());

        chatMessagePublisher.publish(request.getRoomId(), response);
        closeSessions(sessionIds);
    }

    // 탭 닫기, 새로고침, 네트워크 끊김은 실제 퇴장으로 단정하지 않고 서버 세션 정보만 정리한다.
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();

        if (principal == null) {
            return;
        }

        chatSessionRegistry.removeSession(event.getSessionId());
    }

    private User getAuthenticatedUser(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userDetails.getUser();
    }

    private void closeSessions(Set<String> sessionIds) {
        sessionIds.forEach(webSocketSessionStore::close);
    }
}
