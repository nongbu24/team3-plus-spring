package com.example.team3plusspring.domain.chat.repository;

import com.example.team3plusspring.domain.chat.entity.ChatRoom;

import java.util.Optional;

public interface ChatRoomRepositoryCustom {
    Optional<ChatRoom> findByIdWithLock(Long roomId);
}
