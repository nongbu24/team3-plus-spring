package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.entity.ChatMessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private String content;
    private Long senderId;
    private String senderName;
    private ChatMessageType messageType;
    private LocalDateTime createdAt;

    public ChatMessageResponse(Long messageId, String content, Long senderId, String senderName, LocalDateTime createdAt) {
        this(messageId, content, senderId, senderName, ChatMessageType.CHAT, createdAt);
    }

    public ChatMessageResponse(
            Long messageId,
            String content,
            Long senderId,
            String senderName,
            ChatMessageType messageType,
            LocalDateTime createdAt
    ) {
        this.messageId = messageId;
        this.content = content;
        this.senderId = senderId;
        this.senderName = senderName;
        this.messageType = messageType;
        this.createdAt = createdAt;
    }

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

    public static ChatMessageResponse system(String content) {
        return new ChatMessageResponse(null, content, null, "SYSTEM", ChatMessageType.SYSTEM, LocalDateTime.now());
    }
}
