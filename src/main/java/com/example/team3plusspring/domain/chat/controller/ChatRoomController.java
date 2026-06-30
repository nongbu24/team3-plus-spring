package com.example.team3plusspring.domain.chat.controller;

import com.example.team3plusspring.domain.chat.dto.ChatRoomResponse;
import com.example.team3plusspring.domain.chat.dto.ChatRoomStatusRequest;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import com.example.team3plusspring.domain.chat.service.ChatRoomService;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.response.PageResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
@Validated
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    /**
     * 로그인한 일반 사용자의 1:1 문의 채팅방을 생성한다.
     * 관리자 계정은 문의방을 만들 수 없고, 생성된 방에는 요청한 사용자가 참여자로 등록된다.
     *
     * @param userDetails 인증된 사용자 정보
     * @return 생성된 채팅방 정보
     */
    @PostMapping("/me")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> createMyRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ChatRoomResponse response = chatRoomService.createMyRoom(userDetails.getUser());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 채팅방 목록을 조회한다.
     * 관리자는 전체 채팅방을 조회하고, 일반 사용자는 자신이 생성한 채팅방만 조회한다.
     *
     * @param userDetails 인증된 사용자 정보
     * @param status 조회할 채팅방 상태 조건
     * @param page 0부터 시작하는 페이지 번호
     * @param size 페이지당 조회할 채팅방 수
     * @return 조건에 맞는 채팅방 목록 페이지
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChatRoomResponse>>> getRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ChatStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Page<ChatRoomResponse> response = chatRoomService.getRooms(userDetails.getUser(), status, page, size);

        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    /**
     * roomId로 채팅방 단건을 조회한다.
     * 관리자는 담당자가 없거나 본인이 담당 중인 채팅방을 조회하고, 일반 사용자는 자신의 채팅방만 조회한다.
     *
     * @param roomId 조회할 채팅방 ID
     * @param userDetails 인증된 사용자 정보
     * @return 조회한 채팅방 정보
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ChatRoomResponse response = chatRoomService.getRoom(roomId, userDetails.getUser());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 관리자가 문의 채팅방의 상태를 변경한다.
     * WAITING에서 IN_PROGRESS로 변경할 때 담당 관리자가 배정되고, COMPLETED로 변경하면 관련 WebSocket 세션을 정리한다.
     *
     * @param roomId 상태를 변경할 채팅방 ID
     * @param userDetails 인증된 관리자 정보
     * @param request 변경할 채팅방 상태 요청 DTO
     * @return 상태가 변경된 채팅방 정보
     */
    @PatchMapping("/{roomId}/status")
    public ResponseEntity<ApiResponse<ChatRoomResponse>> updateStatus(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChatRoomStatusRequest request
    ) {
        ChatRoomResponse response = chatRoomService.updateStatus(roomId, userDetails.getUser(), request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
