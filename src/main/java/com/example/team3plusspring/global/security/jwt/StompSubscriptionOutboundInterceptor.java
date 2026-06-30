package com.example.team3plusspring.global.security.jwt;

import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompSubscriptionOutboundInterceptor implements ChannelInterceptor {
    private static final String CHAT_SUBSCRIBE_PREFIX = "/sub/chat/";

    private final ChatSessionRegistry chatSessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        SimpMessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor.class);

        if (accessor == null || accessor.getMessageType() != SimpMessageType.MESSAGE) {
            return message;
        }

        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination == null || !destination.startsWith(CHAT_SUBSCRIBE_PREFIX)) {
            return message;
        }

        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        Long roomId = getRoomId(destination);

        if (!chatSessionRegistry.isSubscribed(sessionId, roomId)) {
            return null;
        }

        return message;
    }

    private Long getRoomId(String destination) {
        try {
            return Long.valueOf(destination.substring(CHAT_SUBSCRIBE_PREFIX.length()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
