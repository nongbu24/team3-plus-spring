package com.example.team3plusspring.domain.chat.entity;

import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.global.entity.BaseEntity;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private String customerName;

    private Long adminId;
    private String adminName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatStatus status;

    public ChatRoom(User customer) {
        this.name = customer.getName() + "님의 1:1 문의";
        this.customerId = customer.getId();
        this.customerName = customer.getName();
        this.status = ChatStatus.WAITING;
    }

    public void validateAccess(User user) {
        if (isCustomer(user)) {
            return;
        }

        if (isAccessibleAdmin(user)) {
            return;
        }

        throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    public void validateNotCompleted() {
        if (status == ChatStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_COMPLETED);
        }
    }

    private boolean isCustomer(User user) {
        return customerId.equals(user.getId());
    }

    private boolean isAccessibleAdmin(User user) {
        return user.getRole() == UserRole.ADMIN
                && (adminId == null || adminId.equals(user.getId()));
    }

    public void assignAdmin(User admin) {
        if (admin.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }

        if (adminId == null) {
            this.adminId = admin.getId();
            this.adminName = admin.getName();

            return;
        }

        if (!adminId.equals(admin.getId())) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
        }
    }

    public void changeStatus(ChatStatus nextStatus) {
        if (!this.status.canChangeTo(nextStatus)) {
            throw new BusinessException(ErrorCode.INVALID_CHAT_STATUS_TRANSITION);
        }

        this.status = nextStatus;
    }
}
