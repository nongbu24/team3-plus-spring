package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatRoomResponse;
import com.example.team3plusspring.domain.chat.dto.UpdateChatRoomStatusRequest;
import com.example.team3plusspring.domain.chat.entity.ChatMember;
import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import com.example.team3plusspring.domain.chat.repository.ChatMemberRepository;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatAdminSessionService chatAdminSessionService;

    @Transactional
    public ChatRoomResponse createMyRoom(User user) {
        validateCustomer(user);

        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        chatMemberRepository.save(ChatMember.join(room, user));

        return ChatRoomResponse.from(room);
    }

    public Page<ChatRoomResponse> getRooms(User user, ChatStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (user.getRole() == UserRole.ADMIN) {
            Page<ChatRoom> rooms = status == null
                    ? chatRoomRepository.findAll(pageable)
                    : chatRoomRepository.findAllByStatus(status, pageable);

            return rooms.map(ChatRoomResponse::from);
        }

        Page<ChatRoom> rooms = status == null
                ? chatRoomRepository.findAllByCustomerId(user.getId(), pageable)
                : chatRoomRepository.findAllByCustomerIdAndStatus(user.getId(), status, pageable);

        return rooms.map(ChatRoomResponse::from);
    }

    public void validateRoomAccess(Long roomId, User user) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        room.validateAccess(user);
    }

    @Transactional
    public ChatRoomResponse updateStatus(Long roomId, User user, UpdateChatRoomStatusRequest request) {
        validateAdmin(user);

        ChatRoom room = chatRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        room.validateAccess(user);
        Long assignedAdminId = assignAdminWhenStartProgress(room, user, request.getStatus());
        room.changeStatus(request.getStatus());

        if (assignedAdminId != null) {
            handleAdminAssignedAfterCommit(room.getId(), assignedAdminId);
        }

        return ChatRoomResponse.from(room);
    }

    private void validateCustomer(User user) {
        if (user.getRole() != UserRole.USER) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validateAdmin(User user) {
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private Long assignAdminWhenStartProgress(ChatRoom room, User user, ChatStatus nextStatus) {
        if (room.getStatus() == ChatStatus.WAITING && nextStatus == ChatStatus.IN_PROGRESS) {
            room.assignAdmin(user);
            joinIfNeeded(room, user);

            return user.getId();
        }

        return null;
    }

    private void joinIfNeeded(ChatRoom room, User user) {
        chatMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
                .ifPresentOrElse(
                        ChatMember::rejoin,
                        () -> chatMemberRepository.save(ChatMember.join(room, user))
                );
    }

    private void handleAdminAssignedAfterCommit(Long roomId, Long assignedAdminId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatAdminSessionService.handleAdminAssigned(roomId, assignedAdminId);
            }
        });
    }

}
