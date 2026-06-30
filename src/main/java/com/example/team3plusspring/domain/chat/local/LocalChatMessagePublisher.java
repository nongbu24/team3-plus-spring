package com.example.team3plusspring.domain.chat.local;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.port.ChatMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!redis-chat")
@RequiredArgsConstructor
public class LocalChatMessagePublisher implements ChatMessagePublisher {
    private static final String CHAT_TOPIC_PREFIX = "/sub/chat/";

    private final SimpMessagingTemplate messagingTemplate;

    // Redis를 쓰지 않는 단일 서버 모드에서는 현재 서버에 연결된 WebSocket 구독자에게 바로 보낸다.
    @Override
    public void publish(Long roomId, ChatMessageResponse message) {
        messagingTemplate.convertAndSend(CHAT_TOPIC_PREFIX + roomId, message);
    }
}
