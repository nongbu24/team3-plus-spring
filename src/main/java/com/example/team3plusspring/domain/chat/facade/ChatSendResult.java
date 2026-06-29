package com.example.team3plusspring.domain.chat.facade;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;

public record ChatSendResult(
        ChatMessageResponse message,
        Long assignedAdminId
) {
    public boolean hasNewAssignedAdmin() {
        return assignedAdminId != null;
    }
}
