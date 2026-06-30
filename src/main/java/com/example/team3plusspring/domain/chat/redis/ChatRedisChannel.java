package com.example.team3plusspring.domain.chat.redis;

// Redis Pub/Sub에서 채팅방별 채널 이름을 한 곳에서 관리한다.
public final class ChatRedisChannel {
    public static final String TOPIC_PATTERN = "chat-room:*";
    public static final String ADMIN_ASSIGNED_TOPIC_PATTERN = "chat-admin-assigned:*";
    public static final String SESSION_EXPIRED_TOPIC_PATTERN = "chat-session-expired:*";
    private static final String TOPIC_PREFIX = "chat-room:";
    private static final String ADMIN_ASSIGNED_TOPIC_PREFIX = "chat-admin-assigned:";
    private static final String SESSION_EXPIRED_TOPIC_PREFIX = "chat-session-expired:";

    private ChatRedisChannel() {
    }

    public static String topic(Long roomId) {
        return TOPIC_PREFIX + roomId;
    }

    public static Long roomId(String topic) {
        return Long.valueOf(topic.substring(TOPIC_PREFIX.length()));
    }

    public static String adminAssignedTopic(Long roomId) {
        return ADMIN_ASSIGNED_TOPIC_PREFIX + roomId;
    }

    public static String sessionExpiredTopic(Long roomId) {
        return SESSION_EXPIRED_TOPIC_PREFIX + roomId;
    }
}
