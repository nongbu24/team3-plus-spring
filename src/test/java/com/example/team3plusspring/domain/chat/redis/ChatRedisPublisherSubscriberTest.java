package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.service.ChatAdminSessionService;
import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRedisPublisherSubscriberTest {

    @Mock
    RedisTemplate<String, ChatMessageResponse> chatRedisTemplate;

    @Mock
    RedisTemplate<String, ChatAdminAssignedEvent> chatAdminAssignedRedisTemplate;

    @Mock
    RedisTemplate<String, ChatSessionExpiredEvent> chatSessionExpiredRedisTemplate;

    @Mock
    RedisSerializer<ChatMessageResponse> chatMessageRedisSerializer;

    @Mock
    RedisSerializer<ChatAdminAssignedEvent> chatAdminAssignedRedisSerializer;

    @Mock
    RedisSerializer<ChatSessionExpiredEvent> chatSessionExpiredRedisSerializer;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    ChatAdminSessionService chatAdminSessionService;

    @Mock
    ChatSessionRegistry chatSessionRegistry;

    @Mock
    Message message;

    @Test
    void 레디스발행_방아이디로채널을만들어메시지를발행한다() {
        // given
        ChatRedisPublisher publisher = new ChatRedisPublisher(chatRedisTemplate);
        ChatMessageResponse response = response();

        // when
        publisher.publish(1L, response);

        // then
        verify(chatRedisTemplate).convertAndSend("chat-room:1", response);
    }

    @Test
    void 담당자배정_레디스로배정이벤트를발행한다() {
        // given
        ChatAdminAssignedRedisPublisher publisher = new ChatAdminAssignedRedisPublisher(chatAdminAssignedRedisTemplate);

        // when
        publisher.publish(1L, 10L);

        // then
        verify(chatAdminAssignedRedisTemplate).convertAndSend(
                "chat-admin-assigned:1",
                new ChatAdminAssignedEvent(1L, 10L)
        );
    }

    @Test
    void 만료세션_레디스로만료이벤트를발행한다() {
        // given
        ChatSessionExpiredRedisPublisher publisher = new ChatSessionExpiredRedisPublisher(chatSessionExpiredRedisTemplate);

        // when
        publisher.publish(1L, 10L);

        // then
        verify(chatSessionExpiredRedisTemplate).convertAndSend(
                "chat-session-expired:1",
                new ChatSessionExpiredEvent(1L, 10L)
        );
    }

    @Test
    void 레디스구독_메시지를역직렬화해서웹소켓구독자에게전달한다() {
        // given
        ChatRedisSubscriber subscriber = new ChatRedisSubscriber(chatMessageRedisSerializer, messagingTemplate);
        ChatMessageResponse response = response();
        byte[] body = "body".getBytes(StandardCharsets.UTF_8);

        when(message.getChannel()).thenReturn("chat-room:1".getBytes(StandardCharsets.UTF_8));
        when(message.getBody()).thenReturn(body);
        when(chatMessageRedisSerializer.deserialize(body)).thenReturn(response);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(messagingTemplate).convertAndSend("/sub/chat/1", response);
    }

    @Test
    void 레디스구독_역직렬화결과가없으면_전달하지않는다() {
        // given
        ChatRedisSubscriber subscriber = new ChatRedisSubscriber(chatMessageRedisSerializer, messagingTemplate);
        byte[] body = "body".getBytes(StandardCharsets.UTF_8);

        when(message.getChannel()).thenReturn("chat-room:1".getBytes(StandardCharsets.UTF_8));
        when(message.getBody()).thenReturn(body);
        when(chatMessageRedisSerializer.deserialize(body)).thenReturn(null);

        // when
        subscriber.onMessage(message, null);

        // then
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void 담당자배정구독_현재서버의다른관리자세션을정리한다() {
        // given
        ChatAdminAssignedRedisSubscriber subscriber = new ChatAdminAssignedRedisSubscriber(
                chatAdminAssignedRedisSerializer,
                chatAdminSessionService
        );
        ChatAdminAssignedEvent event = new ChatAdminAssignedEvent(1L, 10L);
        byte[] body = "body".getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(body);
        when(chatAdminAssignedRedisSerializer.deserialize(body)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(chatAdminSessionService).closeOtherAdminSessions(1L, 10L);
    }

    @Test
    void 만료세션구독_현재서버의로컬세션만정리한다() {
        // given
        ChatSessionExpiredRedisSubscriber subscriber = new ChatSessionExpiredRedisSubscriber(
                chatSessionExpiredRedisSerializer,
                chatSessionRegistry
        );
        ChatSessionExpiredEvent event = new ChatSessionExpiredEvent(1L, 10L);
        byte[] body = "body".getBytes(StandardCharsets.UTF_8);

        when(message.getBody()).thenReturn(body);
        when(chatSessionExpiredRedisSerializer.deserialize(body)).thenReturn(event);

        // when
        subscriber.onMessage(message, null);

        // then
        verify(chatSessionRegistry).removeLocalSessions(10L, 1L);
    }

    private ChatMessageResponse response() {
        return ChatMessageResponse.of(
                1L,
                "안녕하세요",
                10L,
                "홍길동",
                LocalDateTime.of(2026, 6, 25, 10, 30)
        );
    }
}
