package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.port.ChatMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class ChatRedisPublisher implements ChatMessagePublisher {
    private final RedisTemplate<String, ChatMessageResponse> chatRedisTemplate;

    @Override
    public void publish(Long roomId, ChatMessageResponse message) {
        chatRedisTemplate.convertAndSend(ChatRedisChannel.topic(roomId), message);
    }
}
