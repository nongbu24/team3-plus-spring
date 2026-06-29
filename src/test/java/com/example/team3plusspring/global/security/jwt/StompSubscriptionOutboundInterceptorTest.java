package com.example.team3plusspring.global.security.jwt;

import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompSubscriptionOutboundInterceptorTest {

    private final ChatSessionRegistry chatSessionRegistry = mock(ChatSessionRegistry.class);
    private final StompSubscriptionOutboundInterceptor interceptor =
            new StompSubscriptionOutboundInterceptor(chatSessionRegistry);

    @Test
    void 채팅방구독이유효하면_메시지를그대로전달한다() {
        // given
        Message<byte[]> message = message("session-1", "/sub/chat/10");
        when(chatSessionRegistry.isSubscribed("session-1", 10L)).thenReturn(true);

        // when
        Message<?> result = interceptor.preSend(message, null);

        // then
        assertThat(result).isSameAs(message);
    }

    @Test
    void 채팅방구독이무효화되면_해당방메시지를차단한다() {
        // given
        Message<byte[]> message = message("session-1", "/sub/chat/10");
        when(chatSessionRegistry.isSubscribed("session-1", 10L)).thenReturn(false);

        // when
        Message<?> result = interceptor.preSend(message, null);

        // then
        assertThat(result).isNull();
    }

    private Message<byte[]> message(String sessionId, String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setDestination(destination);
        accessor.setLeaveMutable(true);

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
