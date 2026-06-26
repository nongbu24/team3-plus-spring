package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.repository.ChatMessageRepository;
import com.example.team3plusspring.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatQueryService {
    private final ChatMessageRepository messageRepository;
    private final ChatRoomService chatRoomService;

    public List<ChatMessageResponse> getRecentMessages(int size) {
        return toResponses(messageRepository.findRecentMessages(pageable(size)));
    }

    public List<ChatMessageResponse> getMessagesBeforeByRoom(Long roomId, Long lastMessageId, User user, int size) {
        chatRoomService.validateRoomAccess(roomId, user);

        return toResponses(messageRepository.findMessagesBeforeByRoom(roomId, lastMessageId, pageable(size)));
    }

    public List<ChatMessageResponse> getMessagesAfterByRoom(Long roomId, Long lastReceivedMessageId, User user, int size) {
        chatRoomService.validateRoomAccess(roomId, user);

        return toResponses(messageRepository.findMessagesAfterByRoom(roomId, lastReceivedMessageId, pageable(size)));
    }

    public List<ChatMessageResponse> getRecentMessagesByRoom(Long roomId, User user, int size) {
        chatRoomService.validateRoomAccess(roomId, user);

        return toResponses(messageRepository.findRecentByRoom(roomId, pageable(size)));
    }

    private Pageable pageable(int size) {
        return PageRequest.of(0, size);
    }

    private List<ChatMessageResponse> toResponses(List<ChatMessage> messages) {
        return messages
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }
}
