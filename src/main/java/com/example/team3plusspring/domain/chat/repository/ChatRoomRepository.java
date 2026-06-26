package com.example.team3plusspring.domain.chat.repository;

import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {
    List<ChatRoom> findAllByCustomerId(Long customerId);

    List<ChatRoom> findAllByCustomerIdAndStatus(Long customerId, ChatStatus status);

    List<ChatRoom> findAllByStatus(ChatStatus status);
}
