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
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatFacade {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, User sender) {
        ChatRoom chatRoom = getAccessibleRoom(request.getRoomId(), sender, true);
        startProgressIfAdminSendsFirstMessage(chatRoom, sender);

        ChatMessage message = ChatMessage.create(sender.getId(), sender.getName(), chatRoom, request.getContent());
        ChatMessage savedMessage = chatMessageRepository.save(message);

        return ChatMessageResponse.from(savedMessage);
    }

    @Transactional
    public ChatMessageResponse enterRoom(Long roomId, User user) {
        ChatRoom chatRoom = getAccessibleRoom(roomId, user, true);

        return saveSystemMessage(chatRoom, user, user.getName() + "님이 입장했습니다");
    }

    @Transactional
    public ChatMessageResponse leaveRoom(Long roomId, User user) {
        ChatRoom chatRoom = getAccessibleRoom(roomId, user, false);
        leaveIfJoined(chatRoom, user);

        return saveSystemMessage(chatRoom, user, user.getName() + "님이 퇴장했습니다");
    }

    private ChatMessageResponse saveSystemMessage(ChatRoom chatRoom, User user, String content) {
        ChatMessage message = ChatMessage.create(user.getId(), user.getName(), chatRoom, content);
        ChatMessage savedMessage = chatMessageRepository.save(message);

        return ChatMessageResponse.from(savedMessage);
    }

    private ChatRoom getAccessibleRoom(Long roomId, User sender, boolean join) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        chatRoom.validateAccess(sender);
        chatRoom.validateNotCompleted();
        if (join) {
            joinIfNeeded(chatRoom, sender);
        }

        return chatRoom;
    }

    private void joinIfNeeded(ChatRoom room, User user) {
        chatMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
                .ifPresentOrElse(
                        ChatMember::rejoin,
                        () -> chatMemberRepository.save(ChatMember.join(room, user))
                );
    }

    private void leaveIfJoined(ChatRoom room, User user) {
        chatMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
                .ifPresent(ChatMember::leave);
    }

    private void startProgressIfAdminSendsFirstMessage(ChatRoom room, User sender) {
        if (sender.getRole() == UserRole.ADMIN && room.getStatus() == ChatStatus.WAITING) {
            room.assignAdmin(sender);
            room.changeStatus(ChatStatus.IN_PROGRESS);
        }
    }
}
