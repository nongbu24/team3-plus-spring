package com.example.team3plusspring.domain.chat.local;

import com.example.team3plusspring.domain.chat.port.ChatAdminAssignmentEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!redis-chat")
public class NoOpChatAdminAssignmentEventPublisher implements ChatAdminAssignmentEventPublisher {

    @Override
    public void publish(Long roomId, Long assignedAdminId) {
    }
}
