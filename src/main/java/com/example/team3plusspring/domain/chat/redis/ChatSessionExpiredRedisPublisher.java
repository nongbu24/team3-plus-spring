package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.port.ChatSessionExpiredEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class ChatSessionExpiredRedisPublisher implements ChatSessionExpiredEventPublisher {
    @Qualifier("chatSessionExpiredRedisTemplate")
    private final RedisTemplate<String, ChatSessionExpiredEvent> chatSessionExpiredRedisTemplate;

    @Override
    public void publish(Long roomId, Long userId) {
        ChatSessionExpiredEvent event = new ChatSessionExpiredEvent(roomId, userId);
        chatSessionExpiredRedisTemplate.convertAndSend(ChatRedisChannel.sessionExpiredTopic(roomId), event);
    }
}
