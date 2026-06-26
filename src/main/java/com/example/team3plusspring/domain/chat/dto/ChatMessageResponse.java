package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private String content;
    private Long senderId;
    private String senderName;
    private LocalDateTime createdAt;

    public ChatMessageResponse(ChatMessage message) {
        this.messageId = message.getId();
        this.content = message.getContent();
        this.senderId = message.getSenderId();
        this.senderName = message.getSenderName();
        this.createdAt = message.getCreatedAt();
    }
}
