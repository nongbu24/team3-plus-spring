package com.example.team3plusspring.domain.chat.entity;

import com.example.team3plusspring.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private String senderName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageType messageType;

    private ChatMessage(Long senderId, String senderName, ChatRoom chatRoom, String content, ChatMessageType messageType) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.chatRoom = chatRoom;
        this.content = content;
        this.messageType = messageType;
    }

    public static ChatMessage create(Long senderId, String senderName, ChatRoom chatRoom, String content) {
        return new ChatMessage(senderId, senderName, chatRoom, content, ChatMessageType.CHAT);
    }

    public static ChatMessage system(Long senderId, String senderName, ChatRoom chatRoom, String content) {
        return new ChatMessage(senderId, senderName, chatRoom, content, ChatMessageType.SYSTEM);
    }
}
