package com.example.team3plusspring.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "chat_messages")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

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
    private LocalDateTime createdAt;

    public ChatMessage(Long senderId, String senderName, ChatRoom chatRoom, String content) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.chatRoom = chatRoom;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
