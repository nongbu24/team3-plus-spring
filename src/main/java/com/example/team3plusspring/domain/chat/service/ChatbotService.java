package com.example.team3plusspring.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final ChatClient chatClient;

    // 세션별 대화 히스토리 저장 (Key: sessionId)
    private final Map<String, List<Message>> historyMap = new ConcurrentHashMap<>();

    public String chat(String sessionId, String userMessage) {

        // 세션 히스토리 가져오기 (없으면 새로 생성)
        List<Message> history = historyMap.computeIfAbsent(
                sessionId, k -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (history) {
            // 히스토리 + 새 질문으로 프롬프트 구성
            String response = chatClient.prompt()
                    .messages(history)              // 이전 대화 내역
                    .user(userMessage)              // 새 질문
                    .call()
                    .content();

            // 히스토리에 이번 대화 추가
            history.add(new UserMessage(userMessage));
            history.add(new AssistantMessage(response));
            trimHistory(history);

            return response;
        }
    }

    // 세션 초기화 (대화 처음부터 다시)
    public void clearHistory(String sessionId) {
        historyMap.remove(sessionId);
    }

    private void trimHistory(List<Message> history) {
        int overflow = history.size() - MAX_HISTORY_MESSAGES;
        if (overflow > 0) {
            history.subList(0, overflow).clear();
        }
    }
}
