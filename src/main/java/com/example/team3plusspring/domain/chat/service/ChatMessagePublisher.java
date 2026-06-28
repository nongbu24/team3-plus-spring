package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;

/**
 * 채팅 메시지를 구독자에게 전달하는 통로를 추상화한다.
 * 로컬 발행과 Redis Pub/Sub 발행을 프로필에 따라 바꿔 끼울 수 있다.
 */
public interface ChatMessagePublisher {
    void publish(Long roomId, ChatMessageResponse message);
}
