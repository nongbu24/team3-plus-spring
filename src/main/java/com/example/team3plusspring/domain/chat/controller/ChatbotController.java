package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatbotRequest;
import com.example.team3plusspring.domain.chat.dto.ChatbotResponse;
import com.example.team3plusspring.domain.chat.service.ChatbotRateLimiter;
import com.example.team3plusspring.domain.chat.service.ChatbotService;
import com.example.team3plusspring.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final ChatbotRateLimiter chatbotRateLimiter;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatbotResponse>> chat(
            @Valid @RequestBody ChatbotRequest request,
            HttpServletRequest httpServletRequest
    ) {
        chatbotRateLimiter.checkAllowed(request.getSessionId(), httpServletRequest);

        String response = chatbotService.chat(
                request.getSessionId(),
                request.getTopic(),
                request.getMessage()
        );

        return ResponseEntity.ok(ApiResponse.success(ChatbotResponse.of(request.getSessionId(), response)));
    }
}
