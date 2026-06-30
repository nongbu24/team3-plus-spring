package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import com.example.team3plusspring.domain.chat.port.ChatMessagePublisher;
import com.example.team3plusspring.domain.chat.port.ChatSessionExpiredEventPublisher;
import com.example.team3plusspring.domain.chat.port.InactiveChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatInactivityService {
    private static final Duration WARNING_AFTER = Duration.ofMinutes(4).plusSeconds(30);
    private static final Duration LEAVE_AFTER = Duration.ofMinutes(5);
    private static final String WARNING_MESSAGE = "일정 시간 동안 채팅 입력이 없다면 자동으로 채팅이 종료됩니다.";

    private final ChatSessionRegistry chatSessionRegistry;
    private final ChatFacade chatFacade;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ChatSessionExpiredEventPublisher chatSessionExpiredEventPublisher;

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void closeInactiveSessions() {
        leaveExpiredSessions();
        publishWarnings();
    }

    private void publishWarnings() {
        chatSessionRegistry.findAndMarkWarningRoomIds(WARNING_AFTER)
                .forEach(roomId -> chatMessagePublisher.publish(roomId, ChatMessageResponse.system(WARNING_MESSAGE)));
    }

    private void leaveExpiredSessions() {
        chatSessionRegistry.expireInactiveSessions(LEAVE_AFTER)
                .forEach(this::leaveExpiredSession);
    }

    private void leaveExpiredSession(InactiveChatSession session) {
        chatFacade.leaveInactiveRoom(session.getRoomId(), session.getUserId())
                .ifPresent(response -> chatMessagePublisher.publish(session.getRoomId(), response));
        chatSessionExpiredEventPublisher.publish(session.getRoomId(), session.getUserId());
    }
}
