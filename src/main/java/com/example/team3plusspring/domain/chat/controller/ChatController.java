package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatRoomEventRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import com.example.team3plusspring.domain.chat.port.ChatMessagePublisher;
import com.example.team3plusspring.domain.chat.port.ChatSessionExpiredEventPublisher;
import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
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

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatFacade chatFacade;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ChatSessionRegistry chatSessionRegistry;
    private final ChatSessionExpiredEventPublisher chatSessionExpiredEventPublisher;

    /**
     * 클라이언트가 /pub/chat.enter로 보낸 "입장" 이벤트를 처리한다.
     * 입장 메시지는 DB에 SYSTEM 메시지로 저장한 뒤 /sub/chat/{roomId} 구독자에게 발행한다.
     *
     * @param request 입장할 채팅방 ID를 담은 요청 DTO
     * @param sessionId 현재 WebSocket 연결을 식별하는 STOMP 세션 ID
     * @param principal STOMP CONNECT 인증을 통과한 사용자 인증 정보
     */
    @MessageMapping("/chat.enter")
    public void enter(
            @Payload @Valid ChatRoomEventRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        User sender = getAuthenticatedUser(principal);

        // 구독하지 않은 방에 enter만 보내는 우회 요청을 막는다.
        if (!chatSessionRegistry.isSubscribed(sessionId, request.getRoomId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        // 같은 세션에서 같은 방 enter가 반복되면 입장 메시지를 중복 저장하지 않는다.
        if (!chatSessionRegistry.enter(sessionId, sender.getId(), request.getRoomId())) {
            return;
        }

        ChatMessageResponse response;

        try {
            response = chatFacade.enterRoom(request.getRoomId(), sender);
        } catch (RuntimeException exception) {
            // DB 처리 중 실패하면 위에서 등록한 입장 상태를 되돌려 세션 상태와 DB 상태를 맞춘다.
            chatSessionRegistry.rollbackEnter(sessionId, sender.getId(), request.getRoomId());

            throw exception;
        }

        chatMessagePublisher.publish(request.getRoomId(), response);
    }

    // 클라이언트가 /pub/chat.send로 보낸 일반 채팅 메시지를 저장하고 구독자에게 발행한다.
    @MessageMapping("/chat.send")
    public void send(
            @Payload @Valid ChatMessageRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        User sender = getAuthenticatedUser(principal);

        // 메시지 전송은 "구독 + 입장"을 모두 마친 세션에서만 허용한다.
        if (!chatSessionRegistry.canSend(sessionId, sender.getId(), request.getRoomId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        ChatMessageResponse response = chatFacade.sendMessage(request, sender);

        // 비활성 자동 퇴장 기준 시간이 메시지 전송 시점부터 다시 계산되도록 갱신한다.
        chatSessionRegistry.refreshRoomActivity(request.getRoomId());
        chatMessagePublisher.publish(request.getRoomId(), response);
    }

    /**
     * 클라이언트가 /pub/chat.leave로 보낸 "명시적 퇴장" 이벤트를 처리한다.
     * 브라우저 탭 닫기 같은 연결 종료와 달리, 사용자가 실제로 퇴장 버튼을 누른 상황으로 본다.
     *
     * @param request 퇴장할 채팅방 ID를 담은 요청 DTO
     * @param sessionId 현재 WebSocket 연결을 식별하는 STOMP 세션 ID
     * @param principal STOMP CONNECT 인증을 통과한 사용자 인증 정보
     */
    @MessageMapping("/chat.leave")
    public void leave(
            @Payload @Valid ChatRoomEventRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        User sender = getAuthenticatedUser(principal);

        // enter를 하지 않은 세션은 퇴장 처리 대상이 아니다.
        if (!chatSessionRegistry.isEntered(sessionId, sender.getId(), request.getRoomId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        ChatMessageResponse response;
        boolean shouldPublishSessionCleanupEvent = false;

        try {
            response = chatFacade.leaveRoom(request.getRoomId(), sender);
            shouldPublishSessionCleanupEvent = true;
        } catch (BusinessException exception) {
            if (exception.getErrorCode() != ErrorCode.CHAT_ROOM_ALREADY_COMPLETED) {
                throw exception;
            }
            // 이미 완료된 방은 퇴장 메시지를 새로 저장하지 않고 세션만 정리한다.
            shouldPublishSessionCleanupEvent = true;

            return;
        } finally {
            // 한 사용자가 같은 방을 여러 탭으로 열 수 있으므로 같은 사용자/방 조합을 모두 정리한다.
            chatSessionRegistry.removeLocalSessions(sender.getId(), request.getRoomId());

            if (shouldPublishSessionCleanupEvent) {
                chatSessionExpiredEventPublisher.publish(request.getRoomId(), sender.getId());
            }
        }

        chatMessagePublisher.publish(request.getRoomId(), response);
    }

    /**
     * 탭 닫기, 새로고침, 네트워크 끊김은 실제 퇴장으로 단정하지 않는다.
     * 그래서 퇴장 메시지는 만들지 않고, 서버가 들고 있던 WebSocket 세션 정보만 정리한다.
     *
     * @param event WebSocket 연결 종료 시 Spring이 발행하는 세션 종료 이벤트
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();

        if (principal == null) {
            return;
        }

        chatSessionRegistry.removeSession(event.getSessionId());
    }

    /**
     * STOMP Principal에서 우리 서비스의 User 객체를 꺼낸다.
     * 인증 정보가 없거나 예상한 타입이 아니면 인증 실패로 처리한다.
     *
     * @param principal STOMP 세션에 연결된 인증 주체
     * @return 인증 주체에서 꺼낸 현재 사용자 엔티티
     */
    private User getAuthenticatedUser(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userDetails.getUser();
    }
}
