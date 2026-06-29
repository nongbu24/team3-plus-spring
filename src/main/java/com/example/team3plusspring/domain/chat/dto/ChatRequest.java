package com.example.team3plusspring.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "세션 ID 입력은 필수입니다.")
        String sessionId,

        @NotBlank(message = "메시지 내용 입력은 필수입니다.")
        @Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다.")
        String message
) {}
