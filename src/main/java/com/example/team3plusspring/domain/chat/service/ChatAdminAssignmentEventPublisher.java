package com.example.team3plusspring.domain.chat.service;

/**
 * 담당 관리자 배정 사실을 다른 서버 인스턴스에 알리는 통로다.
 * 단일 서버 모드에서는 구현체가 아무 작업도 하지 않는다.
 */
public interface ChatAdminAssignmentEventPublisher {
    void publish(Long roomId, Long assignedAdminId);
}
