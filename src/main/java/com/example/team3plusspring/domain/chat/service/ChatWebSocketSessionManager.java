package com.example.team3plusspring.domain.chat.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketSessionManager {
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void unregister(String sessionId) {
        sessions.remove(sessionId);
    }

    public void closeSessions(Set<String> sessionIds) {
        sessionIds.forEach(this::closeSession);
    }

    private void closeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);

        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.close(new CloseStatus(CloseStatus.POLICY_VIOLATION.getCode(), "Admin assignment changed"));
        } catch (IOException ignored) {
        }
    }
}
