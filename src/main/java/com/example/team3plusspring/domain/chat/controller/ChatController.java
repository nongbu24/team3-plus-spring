package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatRoomEventRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import com.example.team3plusspring.domain.chat.service.ChatMessagePublisher;
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
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatFacade chatFacade;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ChatSessionRegistry chatSessionRegistry;

    @MessageMapping("/chat.enter")
    public void enter(
            @Payload @Valid ChatRoomEventRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        User sender = getAuthenticatedUser(principal);
        ChatMessageResponse response = chatFacade.enterRoom(request.getRoomId(), sender);
        chatSessionRegistry.enter(sessionId, request.getRoomId());

        chatMessagePublisher.publish(request.getRoomId(), response);
    }

    @MessageMapping("/chat.send")
    public void send(@Payload @Valid ChatMessageRequest request, Principal principal) {
        User sender = getAuthenticatedUser(principal);
        ChatMessageResponse response = chatFacade.sendMessage(request, sender);

        chatMessagePublisher.publish(request.getRoomId(), response);
    }

    @MessageMapping("/chat.leave")
    public void leave(
            @Payload @Valid ChatRoomEventRequest request,
            @Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        User sender = getAuthenticatedUser(principal);
        ChatMessageResponse response = chatFacade.leaveRoom(request.getRoomId(), sender);
        chatSessionRegistry.leave(sessionId, request.getRoomId());

        chatMessagePublisher.publish(request.getRoomId(), response);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) {
            return;
        }

        User sender = getAuthenticatedUser(principal);
        Set<Long> roomIds = chatSessionRegistry.removeSession(event.getSessionId());

        for (Long roomId : roomIds) {
            try {
                ChatMessageResponse response = chatFacade.leaveRoom(roomId, sender);
                chatMessagePublisher.publish(roomId, response);
            } catch (BusinessException exception) {
                if (exception.getErrorCode() != ErrorCode.CHAT_ROOM_ALREADY_COMPLETED) {
                    throw exception;
                }
            }
        }
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
}
