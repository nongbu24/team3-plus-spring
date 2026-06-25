package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatRoomResponse;
import com.example.team3plusspring.domain.chat.dto.UpdateChatRoomStatusRequest;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import com.example.team3plusspring.domain.chat.service.ChatRoomService;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createMyRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ChatRoomResponse response = chatRoomService.createMyRoom(userDetails.getUser());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ChatStatus status
    ) {
        List<ChatRoomResponse> response = chatRoomService.getRooms(userDetails.getUser(), status);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{roomId}/status")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> updateStatus(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateChatRoomStatusRequest request
    ) {
        ChatRoomResponse response = chatRoomService.updateStatus(roomId, userDetails.getUser(), request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
