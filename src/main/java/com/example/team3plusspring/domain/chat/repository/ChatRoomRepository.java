package com.example.team3plusspring.domain.chat.repository;

import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomRepositoryCustom {
    Page<ChatRoom> findAllByCustomerId(Long customerId, Pageable pageable);

    Page<ChatRoom> findAllByCustomerIdAndStatus(Long customerId, ChatStatus status, Pageable pageable);

    Page<ChatRoom> findAllByStatus(ChatStatus status, Pageable pageable);
}
