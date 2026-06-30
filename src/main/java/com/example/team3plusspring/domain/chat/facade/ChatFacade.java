package com.example.team3plusspring.domain.chat.facade;

import com.example.team3plusspring.domain.chat.dto.ChatMessageRequest;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.entity.ChatMember;
import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import com.example.team3plusspring.domain.chat.repository.ChatMemberRepository;
import com.example.team3plusspring.domain.chat.repository.ChatMessageRepository;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.chat.service.ChatAdminSessionService;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatFacade {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatAdminSessionService chatAdminSessionService;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, User sender) {
        ChatRoom chatRoom = findOpenRoom(request.getRoomId(), sender, true);
        Long assignedAdminId = assignAdminIfWaiting(chatRoom, sender);

        ChatMessage message = ChatMessage.create(sender.getId(), sender.getName(), chatRoom, request.getContent());
        ChatMessage savedMessage = chatMessageRepository.save(message);

        notifyAdminAssigned(chatRoom.getId(), assignedAdminId);

        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional
    public ChatMessageResponse enterRoom(Long roomId, User user) {
        ChatRoom chatRoom = findOpenRoom(roomId, user, true);
        Long assignedAdminId = assignAdminIfWaiting(chatRoom, user);
        ChatMessageResponse response = saveSystemMessage(chatRoom, user, user.getName() + "님이 입장했습니다");

        notifyAdminAssigned(chatRoom.getId(), assignedAdminId);

        return response;
    }

    @Transactional
    public ChatMessageResponse leaveRoom(Long roomId, User user) {
        ChatRoom chatRoom = findOpenRoom(roomId, user, false);
        leaveIfJoined(chatRoom, user);

        return saveSystemMessage(chatRoom, user, user.getName() + "님이 퇴장했습니다");
    }

    @Transactional
    public Optional<ChatMessageResponse> leaveInactiveRoom(Long roomId, Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .flatMap(user -> leaveInactiveRoom(roomId, user));
    }

    private Optional<ChatMessageResponse> leaveInactiveRoom(Long roomId, User user) {
        try {
            ChatRoom chatRoom = findOpenRoom(roomId, user, false);

            if (!leaveIfJoined(chatRoom, user)) {
                return Optional.empty();
            }

            return Optional.of(saveSystemMessage(chatRoom, user, user.getName() + "님이 퇴장했습니다"));
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.CHAT_ROOM_ALREADY_COMPLETED) {
                return Optional.empty();
            }

            throw exception;
        }
    }

    private ChatMessageResponse saveSystemMessage(ChatRoom chatRoom, User user, String content) {
        ChatMessage message = ChatMessage.system(user.getId(), user.getName(), chatRoom, content);
        ChatMessage savedMessage = chatMessageRepository.save(message);

        return ChatMessageResponse.from(savedMessage);
    }

    private ChatRoom findOpenRoom(Long roomId, User sender, boolean join) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.validateAccess(sender);
        chatRoom.validateOpen();

        if (join && shouldJoinAsMember(chatRoom, sender)) {
            joinIfNeeded(chatRoom, sender);
        }

        return chatRoom;
    }

    private boolean shouldJoinAsMember(ChatRoom room, User user) {
        return room.getCustomerId().equals(user.getId())
                || (room.getAdminId() != null && room.getAdminId().equals(user.getId()));
    }

    private void joinIfNeeded(ChatRoom room, User user) {
        chatMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
                .ifPresentOrElse(
                        ChatMember::rejoin,
                        () -> chatMemberRepository.save(ChatMember.join(room, user))
                );
    }

    private boolean leaveIfJoined(ChatRoom room, User user) {
        return chatMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
                .map(ChatMember::leave)
                .orElse(false);
    }

    private Long assignAdminIfWaiting(ChatRoom room, User sender) {
        if (sender.getRole() == UserRole.ADMIN && room.getStatus() == ChatStatus.WAITING) {
            room.assignAdmin(sender);
            room.changeTo(ChatStatus.IN_PROGRESS);
            joinIfNeeded(room, sender);

            return sender.getId();
        }

        return null;
    }

    private void notifyAdminAssigned(Long roomId, Long assignedAdminId) {
        if (assignedAdminId == null) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chatAdminSessionService.handleAdminAssigned(roomId, assignedAdminId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatAdminSessionService.handleAdminAssigned(roomId, assignedAdminId);
            }
        });
    }
}
