package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {
    private static final String CHAT_TOPIC_PREFIX = "/sub/chat/";

    private final RedisSerializer<ChatMessageResponse> chatMessageRedisSerializer;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel(), StandardCharsets.UTF_8);
        ChatMessageResponse response = chatMessageRedisSerializer.deserialize(message.getBody());

        if (response == null) {
            return;
        }

        Long roomId = ChatRedisChannel.roomId(topic);
        messagingTemplate.convertAndSend(CHAT_TOPIC_PREFIX + roomId, response);
    }
}
