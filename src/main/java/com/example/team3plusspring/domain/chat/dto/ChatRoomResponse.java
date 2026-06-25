package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatRoomResponse {
    private final Long roomId;
    private final String name;
    private final Long customerId;
    private final String customerName;
    private final Long adminId;
    private final String adminName;
    private final ChatStatus status;
    private final LocalDateTime createdAt;

    public ChatRoomResponse(ChatRoom room) {
        this.roomId = room.getId();
        this.name = room.getName();
        this.customerId = room.getCustomerId();
        this.customerName = room.getCustomerName();
        this.adminId = room.getAdminId();
        this.adminName = room.getAdminName();
        this.status = room.getStatus();
        this.createdAt = room.getCreatedAt();
    }
}
