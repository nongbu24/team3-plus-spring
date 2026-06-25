package com.example.team3plusspring.domain.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomEventRequest {
    @NotNull(message = "채팅방 ID 입력은 필수입니다.")
    private Long roomId;
}
