package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatRoomStatusRequest {

    @NotNull(message = "문의 상태 입력은 필수입니다.")
    private ChatStatus status;

    public static ChatRoomStatusRequest of(ChatStatus status) {
        return new ChatRoomStatusRequest(status);
    }
}
