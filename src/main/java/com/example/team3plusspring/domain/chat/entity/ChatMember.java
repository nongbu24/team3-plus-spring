package com.example.team3plusspring.domain.chat.entity;

import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "chat_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"room_id", "user_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    private ChatMember(ChatRoom chatRoom, User user) {
        this.chatRoom = chatRoom;
        this.userId = user.getId();
        this.userName = user.getName();
        this.role = user.getRole();
        this.joinedAt = LocalDateTime.now();
    }

    public static ChatMember join(ChatRoom chatRoom, User user) {
        return new ChatMember(chatRoom, user);
    }

    public void rejoin() {
        this.joinedAt = LocalDateTime.now();
        this.leftAt = null;
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }
}
