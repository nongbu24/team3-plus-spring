package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.port.ChatAdminAssignmentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class ChatAdminAssignedRedisPublisher implements ChatAdminAssignmentEventPublisher {

    @Qualifier("chatAdminAssignedRedisTemplate")
    private final RedisTemplate<String, ChatAdminAssignedEvent> chatAdminAssignedRedisTemplate;

    @Override
    public void publish(Long roomId, Long assignedAdminId) {
        ChatAdminAssignedEvent event = new ChatAdminAssignedEvent(roomId, assignedAdminId);
        chatAdminAssignedRedisTemplate.convertAndSend(ChatRedisChannel.adminAssignedTopic(roomId), event);
    }
}
