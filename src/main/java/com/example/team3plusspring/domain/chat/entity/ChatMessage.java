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

    private ChatMessage(Long senderId, String senderName, ChatRoom chatRoom, String content) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.chatRoom = chatRoom;
        this.content = content;
    }

    public static ChatMessage create(Long senderId, String senderName, ChatRoom chatRoom, String content) {
        return new ChatMessage(senderId, senderName, chatRoom, content);
    }
}
