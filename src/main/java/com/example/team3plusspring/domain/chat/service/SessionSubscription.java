package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.user.entity.UserRole;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
class SessionSubscription {
    private final String subscriptionId;
    private final Long userId;
    private final UserRole role;
    private final Long roomId;

    boolean isSameSubscription(String targetSubscriptionId) {
        return subscriptionId != null && subscriptionId.equals(targetSubscriptionId);
    }

    boolean isSameUserRoom(Long targetUserId, Long targetRoomId) {
        return userId.equals(targetUserId) && roomId.equals(targetRoomId);
    }

    boolean isOtherAdmin(Long targetRoomId, Long assignedAdminId) {
        return role == UserRole.ADMIN && roomId.equals(targetRoomId) && !userId.equals(assignedAdminId);
    }
}
