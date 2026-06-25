package com.example.team3plusspring.domain.chat.redis;

public final class ChatRedisChannel {
    public static final String TOPIC_PATTERN = "chat-room:*";
    public static final String TOPIC_PREFIX = "chat-room:";

    private ChatRedisChannel() {
    }

    public static String topic(Long roomId) {
        return TOPIC_PREFIX + roomId;
    }

    public static Long roomId(String topic) {
        return Long.valueOf(topic.substring(TOPIC_PREFIX.length()));
    }
}
