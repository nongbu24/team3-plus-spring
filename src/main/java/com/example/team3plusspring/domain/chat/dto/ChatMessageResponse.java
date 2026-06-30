package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.entity.ChatMessageType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMessageResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private String content;
    private Long senderId;
    private String senderName;
    private ChatMessageType messageType;
    private LocalDateTime createdAt;

    private ChatMessageResponse(ChatMessage message) {
        this.messageId = message.getId();
        this.content = message.getContent();
        this.senderId = message.getSenderId();
        this.senderName = message.getSenderName();
        this.messageType = message.getMessageType();
        this.createdAt = message.getCreatedAt();
    }

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message);
    }

    public static ChatMessageResponse of(
            Long messageId,
            String content,
            Long senderId,
            String senderName,
            LocalDateTime createdAt
    ) {
        return new ChatMessageResponse(messageId, content, senderId, senderName, ChatMessageType.CHAT, createdAt);
    }

    public static ChatMessageResponse of(
            Long messageId,
            String content,
            Long senderId,
            String senderName,
            ChatMessageType messageType,
            LocalDateTime createdAt
    ) {
        return new ChatMessageResponse(messageId, content, senderId, senderName, messageType, createdAt);
    }

    public static ChatMessageResponse system(String content) {
        return new ChatMessageResponse(null, content, null, "SYSTEM", ChatMessageType.SYSTEM, LocalDateTime.now());
    }
}
