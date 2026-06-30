package com.example.team3plusspring.domain.chat.repository;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatMessageRepositoryCustom {
    List<ChatMessage> findRecent(Pageable pageable);

    List<ChatMessage> findBefore(Long roomId, Long messageId, Pageable pageable);

    List<ChatMessage> findAfter(Long roomId, Long messageId, Pageable pageable);

    List<ChatMessage> findRecentByRoom(Long roomId, Pageable pageable);
}
