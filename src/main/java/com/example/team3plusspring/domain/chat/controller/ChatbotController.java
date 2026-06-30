package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatbotRequest;
import com.example.team3plusspring.domain.chat.dto.ChatbotResponse;
import com.example.team3plusspring.domain.chat.service.ChatbotService;
import com.example.team3plusspring.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Validated
public class ChatbotController {
    private final ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatbotResponse>> chat(@Valid @RequestBody ChatbotRequest request) {
        String response = chatbotService.chat(
                request.getSessionId(),
                request.getTopic(),
                request.getMessage()
        );

        return ResponseEntity.ok(ApiResponse.success(ChatbotResponse.of(request.getSessionId(), response)));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @PathVariable
            @Pattern(
                    regexp = ChatbotRequest.SESSION_ID_REGEX,
                    message = "세션 ID 형식이 올바르지 않습니다."
            )
            String sessionId
    ) {
        chatbotService.clearHistory(sessionId);

        return ResponseEntity.ok(ApiResponse.<Void>success(null));
    }
}
