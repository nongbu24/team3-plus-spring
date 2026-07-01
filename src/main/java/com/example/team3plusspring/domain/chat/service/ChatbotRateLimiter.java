package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ChatbotRateLimiter {
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final int MAX_RATE_LIMIT_KEYS = 20_000;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW)
            .maximumSize(MAX_RATE_LIMIT_KEYS)
            .build();

    public void checkAllowed(String sessionId, HttpServletRequest request) {
        String key = clientIp(request) + ":" + sessionId;
        AtomicInteger count = requestCounts.get(key, ignored -> new AtomicInteger());

        if (count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            throw new BusinessException(ErrorCode.CHATBOT_RATE_LIMIT_EXCEEDED);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
