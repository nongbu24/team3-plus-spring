package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;

public interface ChatMessagePublisher {
    void publish(Long roomId, ChatMessageResponse message);
}
