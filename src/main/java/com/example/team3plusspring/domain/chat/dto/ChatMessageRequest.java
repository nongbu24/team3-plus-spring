package com.example.team3plusspring.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    @NotNull(message = "채팅방 ID 입력은 필수입니다.")
    private Long roomId;

    @NotBlank(message = "메시지 내용 입력은 필수입니다.")
    @Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다.")
    private String content;
}
