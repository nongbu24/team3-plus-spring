package com.example.team3plusspring.domain.chat.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionStore {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void add(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public void close(String sessionId) {
        WebSocketSession session = sessions.get(sessionId);

        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.close(CloseStatus.NORMAL);
        } catch (IOException ignored) {
        }
    }
}
