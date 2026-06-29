package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.dto.ChatRoomMessagesResponse;
import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.repository.ChatMessageRepository;
import com.example.team3plusspring.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryService {
    private final ChatMessageRepository messageRepository;
    private final ChatRoomService chatRoomService;

    public List<ChatRoomMessagesResponse> getRecentMessagesGroupedByRoom(int size) {
        Map<Long, List<ChatMessageResponse>> messagesByRoom = new LinkedHashMap<>();

        messageRepository.findRecentMessages(pageable(size))
                .forEach(message -> messagesByRoom
                        .computeIfAbsent(message.getChatRoom().getId(), roomId -> new ArrayList<>())
                        .add(ChatMessageResponse.from(message)));

        return messagesByRoom.entrySet()
                .stream()
                .map(entry -> new ChatRoomMessagesResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<ChatMessageResponse> getMessagesBeforeByRoom(Long roomId, Long lastMessageId, User user, int size) {
        chatRoomService.validateRoomAccess(roomId, user);

        return toResponses(messageRepository.findMessagesBeforeByRoom(roomId, lastMessageId, pageable(size)));
    }

    public List<ChatMessageResponse> getMessagesAfterByRoom(Long roomId, Long lastMessageId, User user, int size) {
        chatRoomService.validateRoomAccess(roomId, user);

        return toResponses(messageRepository.findMessagesAfterByRoom(roomId, lastMessageId, pageable(size)));
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
