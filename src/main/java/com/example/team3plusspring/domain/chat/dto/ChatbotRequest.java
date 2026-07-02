package com.example.team3plusspring.domain.chat.dto;

import com.example.team3plusspring.domain.chat.entity.ChatbotTopic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatbotRequest {
    public static final String SESSION_ID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    @NotBlank(message = "세션 ID 입력은 필수입니다.")
    @Pattern(regexp = SESSION_ID_REGEX, message = "세션 ID 형식이 올바르지 않습니다.")
    private String sessionId;

    @NotNull(message = "상담 유형 선택은 필수입니다.")
    private ChatbotTopic topic;

    @NotBlank(message = "메시지 내용 입력은 필수입니다.")
    @Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다.")
    private String message;

    public static ChatbotRequest of(String sessionId, ChatbotTopic topic, String message) {
        return new ChatbotRequest(sessionId, topic, message);
    }
}
