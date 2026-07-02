package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.dto.ChatRoomMessagesResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Validated
public class ChatQueryController {
    private final ChatQueryService chatQueryService;

    /**
     * 관리자가 전체 채팅방의 최근 메시지를 채팅방별로 묶어서 조회한다.
     * 상담 현황 모니터링용 읽기 API라 담당자가 다른 방의 메시지도 조회할 수 있다.
     * 단, 실제 채팅방 단건 조회, 구독, 답장은 담당자 접근 정책을 따른다.
     *
     * @param size 조회할 최근 메시지 개수
     * @return 채팅방 ID와 해당 방의 최근 메시지 목록
     */
    @GetMapping("/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ChatRoomMessagesResponse>>> getAdminMessages(
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        List<ChatRoomMessagesResponse> response = chatQueryService.getRecentMessagesGroupedByRoom(size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 채팅방에서 기준 메시지보다 오래된 메시지를 조회한다.
     * 채팅 화면을 위로 올려 이전 대화 내용을 더 불러올 때 사용한다.
     *
     * @param roomId 조회할 채팅방 ID
     * @param lastMessageId 현재 화면에서 가장 오래된 메시지 ID
     * @param userDetails 인증된 사용자 정보
     * @param size 조회할 이전 메시지 개수
     * @return 기준 메시지보다 오래된 채팅 메시지 목록
     */
    @GetMapping("/rooms/{roomId}/messages/before/{lastMessageId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessagesBefore(
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

    /**
     * 특정 채팅방에서 기준 메시지보다 새로 도착한 메시지를 조회한다.
     * 재연결 이후 클라이언트가 받지 못한 메시지를 맞춰 가져올 때 사용한다.
     *
     * @param roomId 조회할 채팅방 ID
     * @param lastMessageId 클라이언트가 마지막으로 받은 메시지 ID
     * @param userDetails 인증된 사용자 정보
     * @param size 조회할 이후 메시지 개수
     * @return 기준 메시지보다 나중에 생성된 채팅 메시지 목록
     */
    @GetMapping("/rooms/{roomId}/messages/after/{lastMessageId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessagesAfter(
            @PathVariable Long roomId,
            @PathVariable Long lastMessageId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size
    ) {
        List<ChatMessageResponse> response = chatQueryService.getMessagesAfterByRoom(
                roomId,
                lastMessageId,
                userDetails.getUser(),
                size
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 채팅방의 최근 메시지를 조회한다.
     * 채팅방에 처음 들어왔을 때 화면에 보여줄 초기 메시지 목록으로 사용한다.
     *
     * @param roomId 조회할 채팅방 ID
     * @param userDetails 인증된 사용자 정보
     * @param size 조회할 최근 메시지 개수
     * @return 해당 채팅방의 최근 채팅 메시지 목록
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getRecentMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        List<ChatMessageResponse> response = chatQueryService.getRecentMessagesByRoom(roomId, userDetails.getUser(), size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
