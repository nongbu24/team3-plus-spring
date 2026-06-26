package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
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
    RedisSerializer<ChatMessageResponse> chatMessageRedisSerializer;

    @Mock
    SimpMessagingTemplate messagingTemplate;

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

    private ChatMessageResponse response() {
        return new ChatMessageResponse(
                1L,
                "안녕하세요",
                10L,
                "홍길동",
                LocalDateTime.of(2026, 6, 25, 10, 30)
        );
    }
}
