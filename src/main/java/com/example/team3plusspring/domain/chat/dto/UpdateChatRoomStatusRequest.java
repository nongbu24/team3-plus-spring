package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateChatRoomStatusRequest {
    @NotNull(message = "문의 상태 입력은 필수입니다.")
    private ChatStatus status;
}
