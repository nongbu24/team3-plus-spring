package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomResponse {
    private final Long roomId;
    private final String name;
    private final Long customerId;
    private final String customerName;
    private final Long adminId;
    private final String adminName;
    private final ChatStatus status;
    private final LocalDateTime createdAt;

    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getName(),
                room.getCustomerId(),
                room.getCustomerName(),
                room.getAdminId(),
                room.getAdminName(),
                room.getStatus(),
                room.getCreatedAt()
        );
    }
}
