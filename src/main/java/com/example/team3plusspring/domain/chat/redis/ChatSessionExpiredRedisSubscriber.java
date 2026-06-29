package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class ChatSessionExpiredRedisSubscriber implements MessageListener {
    @Qualifier("chatSessionExpiredRedisSerializer")
    private final RedisSerializer<ChatSessionExpiredEvent> chatSessionExpiredRedisSerializer;
    private final ChatSessionRegistry chatSessionRegistry;

    // 만료 이벤트를 받으면 DB 처리는 하지 않고 현재 서버의 로컬 WebSocket 세션만 정리한다.
    @Override
    public void onMessage(Message message, byte[] pattern) {
        ChatSessionExpiredEvent event = chatSessionExpiredRedisSerializer.deserialize(message.getBody());

        if (event == null) {
            return;
        }

        chatSessionRegistry.removeLocalSessions(event.getUserId(), event.getRoomId());
    }
}
