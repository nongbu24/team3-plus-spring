package com.example.team3plusspring.domain.chat.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!redis-chat")
public class NoOpChatSessionExpiredEventPublisher implements ChatSessionExpiredEventPublisher {
    @Override
    public void publish(Long roomId, Long userId) {
    }
}
