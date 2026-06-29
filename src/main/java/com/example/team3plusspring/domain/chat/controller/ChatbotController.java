package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatRequest;
import com.example.team3plusspring.domain.chat.dto.ChatResponse;
import com.example.team3plusspring.domain.chat.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    private final ChatbotService chatbotService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String response = chatbotService.chat(
                request.sessionId(),
                request.message()
        );
        return new ChatResponse(request.sessionId(), response);
    }

    @DeleteMapping("/{sessionId}")
    public void clearHistory(@PathVariable String sessionId) {
        chatbotService.clearHistory(sessionId);
    }
}
