package com.example.team3plusspring.domain.chat.repository;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatMessageRepositoryCustom {
    List<ChatMessage> findRecentMessages(Pageable pageable);

    List<ChatMessage> findMessagesBeforeByRoom(Long roomId, Long lastMessageId, Pageable pageable);

    List<ChatMessage> findMessagesAfterByRoom(Long roomId, Long lastMessageId, Pageable pageable);

    List<ChatMessage> findRecentByRoom(Long roomId, Pageable pageable);
}
