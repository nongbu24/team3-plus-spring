package com.example.team3plusspring.domain.chat.service;

/**
 * 비활성 만료로 퇴장 확정된 세션을 다른 서버 인스턴스에 알리는 통로다.
 * 각 서버는 이 이벤트를 받아 자기 서버의 로컬 WebSocket 세션만 정리한다.
 */
public interface ChatSessionExpiredEventPublisher {
    void publish(Long roomId, Long userId);
}
