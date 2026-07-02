package com.example.team3plusspring.domain.chat.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatLeaveResult {
    private final ChatMessageResponse message;
    private final Long completedCustomerId;
    private final Long completedAdminId;

    public static ChatLeaveResult of(ChatMessageResponse message) {
        return new ChatLeaveResult(message, null, null);
    }

    public static ChatLeaveResult completed(ChatMessageResponse message, Long customerId, Long adminId) {
        return new ChatLeaveResult(message, customerId, adminId);
    }

    public boolean completedRoom() {
        return completedCustomerId != null;
    }
}
