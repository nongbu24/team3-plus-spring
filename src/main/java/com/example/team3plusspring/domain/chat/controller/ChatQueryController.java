package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.service.ChatQueryService;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Validated
public class ChatQueryController {
    private final ChatQueryService chatQueryService;

    @GetMapping("/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getRecentMessagesForAdmin(
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        List<ChatMessageResponse> response = chatQueryService.getRecentMessages(size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/rooms/{roomId}/messages/before/{lastMessageId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessagesBeforeByRoom(
            @PathVariable Long roomId,
            @PathVariable Long lastMessageId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        List<ChatMessageResponse> response = chatQueryService.getMessagesBeforeByRoom(
                roomId,
                lastMessageId,
                userDetails.getUser(),
                size
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/rooms/{roomId}/messages/after/{lastReceivedMessageId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessagesAfterByRoom(
            @PathVariable Long roomId,
            @PathVariable Long lastReceivedMessageId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size
    ) {
        List<ChatMessageResponse> response = chatQueryService.getMessagesAfterByRoom(
                roomId,
                lastReceivedMessageId,
                userDetails.getUser(),
                size
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getRecentMessagesByRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        List<ChatMessageResponse> response = chatQueryService.getRecentMessagesByRoom(roomId, userDetails.getUser(), size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
