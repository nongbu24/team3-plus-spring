package com.example.team3plusspring.domain.chat.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomMessagesResponse {
    private final Long roomId;
    private final List<ChatMessageResponse> messages;

    public static ChatRoomMessagesResponse of(Long roomId, List<ChatMessageResponse> messages) {
        return new ChatRoomMessagesResponse(roomId, messages);
    }
}
