package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
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

    @Override
    public void publish(Long roomId, ChatMessageResponse message) {
        messagingTemplate.convertAndSend(CHAT_TOPIC_PREFIX + roomId, message);
    }
}
