package com.example.team3plusspring.domain.chat.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatbotResponse {
    private final String sessionId;
    private final String message;

    public static ChatbotResponse of(String sessionId, String message) {
        return new ChatbotResponse(sessionId, message);
    }
}
