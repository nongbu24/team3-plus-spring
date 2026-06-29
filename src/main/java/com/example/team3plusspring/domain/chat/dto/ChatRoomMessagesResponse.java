package com.example.team3plusspring.domain.chat.dto;

import java.util.List;

public record ChatRoomMessagesResponse(
        Long roomId,
        List<ChatMessageResponse> messages
) {
}
