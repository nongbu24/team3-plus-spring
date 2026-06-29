package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.service.ChatAdminSessionService;
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
public class ChatAdminAssignedRedisSubscriber implements MessageListener {
    @Qualifier("chatAdminAssignedRedisSerializer")
    private final RedisSerializer<ChatAdminAssignedEvent> chatAdminAssignedRedisSerializer;
    private final ChatAdminSessionService chatAdminSessionService;

    // 다른 인스턴스에서 담당자가 배정되었다는 이벤트를 받으면, 현재 서버의 다른 관리자 구독만 정리한다.
    @Override
    public void onMessage(Message message, byte[] pattern) {
        ChatAdminAssignedEvent event = chatAdminAssignedRedisSerializer.deserialize(message.getBody());

        if (event == null) {
            return;
        }

        chatAdminSessionService.closeOtherAdminSessions(event.getRoomId(), event.getAssignedAdminId());
    }
}
