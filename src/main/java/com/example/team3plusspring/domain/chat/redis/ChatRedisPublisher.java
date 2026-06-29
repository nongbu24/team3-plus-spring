package com.example.team3plusspring.domain.chat.redis;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.service.ChatMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("redis-chat")
@RequiredArgsConstructor
public class ChatRedisPublisher implements ChatMessagePublisher {
    private final RedisTemplate<String, ChatMessageResponse> chatRedisTemplate;

    /**
     * 서버가 여러 대일 때 한 서버에서 받은 채팅 메시지를 다른 서버들도 알 수 있어야 한다.
     * 그래서 이 클래스는 WebSocket 구독자에게 직접 보내지 않고 Redis 채널에 먼저 메시지를 발행한다.
     * 각 서버는 ChatRedisSubscriber로 같은 Redis 채널을 구독하고 있다가,
     * 메시지를 받으면 자기 서버에 연결된 /sub/chat/{roomId} 구독자에게 WebSocket으로 전달한다.
     */
    @Override
    public void publish(Long roomId, ChatMessageResponse message) {
        chatRedisTemplate.convertAndSend(ChatRedisChannel.topic(roomId), message);
    }
}
