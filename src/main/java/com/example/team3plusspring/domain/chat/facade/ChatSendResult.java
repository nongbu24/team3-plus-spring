package com.example.team3plusspring.domain.chat.facade;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;

public class ChatSendResult {
    private final ChatMessageResponse message;
    private final Long assignedAdminId;

    public ChatSendResult(ChatMessageResponse message, Long assignedAdminId) {
        this.message = message;
        this.assignedAdminId = assignedAdminId;
    }

    public ChatMessageResponse getMessage() {
        return message;
    }

    public Long getAssignedAdminId() {
        return assignedAdminId;
    }

    public boolean hasNewAssignedAdmin() {
        return assignedAdminId != null;
    }
}
