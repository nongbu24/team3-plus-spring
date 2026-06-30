package com.example.team3plusspring.global.config;

import com.example.team3plusspring.domain.chat.redis.ChatAdminAssignedEvent;
import com.example.team3plusspring.domain.chat.redis.ChatAdminAssignedRedisSubscriber;
import com.example.team3plusspring.domain.chat.redis.ChatRedisChannel;
import com.example.team3plusspring.domain.chat.redis.ChatRedisSubscriber;
import com.example.team3plusspring.domain.chat.redis.ChatSessionExpiredEvent;
import com.example.team3plusspring.domain.chat.redis.ChatSessionExpiredRedisSubscriber;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
@Profile("redis-chat")
public class RedisConfig {

    @Bean
    public RedisSerializer<ChatMessageResponse> chatMessageRedisSerializer(ObjectMapper objectMapper) {
        // Redis Pub/Sub 메시지를 Java 기본 직렬화 대신 JSON으로 전달해 사람이 읽기 쉽고 서버 버전 변경에도 비교적 유연하게 처리한다.
        return new JacksonJsonRedisSerializer<>(objectMapper, ChatMessageResponse.class);
    }

    @Bean
    public RedisSerializer<ChatAdminAssignedEvent> chatAdminAssignedRedisSerializer(ObjectMapper objectMapper) {
        // 담당자 배정 이벤트도 Redis를 통해 다른 서버로 전달되므로 JSON 형태로 직렬화한다.
        return new JacksonJsonRedisSerializer<>(objectMapper, ChatAdminAssignedEvent.class);
    }

    @Bean
    public RedisSerializer<ChatSessionExpiredEvent> chatSessionExpiredRedisSerializer(ObjectMapper objectMapper) {
        // 비활성 만료 이벤트도 모든 서버가 받아 로컬 WebSocket 세션을 정리할 수 있게 JSON으로 전달한다.
        return new JacksonJsonRedisSerializer<>(objectMapper, ChatSessionExpiredEvent.class);
    }

    @Bean
    public RedisTemplate<String, ChatMessageResponse> chatRedisTemplate(
            RedisConnectionFactory connectionFactory,
            @Qualifier("chatMessageRedisSerializer") RedisSerializer<ChatMessageResponse> chatMessageRedisSerializer
    ) {
        return createRedisTemplate(connectionFactory, chatMessageRedisSerializer);
    }

    @Bean
    public RedisTemplate<String, ChatAdminAssignedEvent> chatAdminAssignedRedisTemplate(
            RedisConnectionFactory connectionFactory,
            @Qualifier("chatAdminAssignedRedisSerializer") RedisSerializer<ChatAdminAssignedEvent> chatAdminAssignedRedisSerializer
    ) {
        return createRedisTemplate(connectionFactory, chatAdminAssignedRedisSerializer);
    }

    @Bean
    public RedisTemplate<String, ChatSessionExpiredEvent> chatSessionExpiredRedisTemplate(
            RedisConnectionFactory connectionFactory,
            @Qualifier("chatSessionExpiredRedisSerializer") RedisSerializer<ChatSessionExpiredEvent> chatSessionExpiredRedisSerializer
    ) {
        return createRedisTemplate(connectionFactory, chatSessionExpiredRedisSerializer);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatRedisSubscriber chatRedisSubscriber,
            ChatAdminAssignedRedisSubscriber chatAdminAssignedRedisSubscriber,
            ChatSessionExpiredRedisSubscriber chatSessionExpiredRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatRedisSubscriber, new PatternTopic(ChatRedisChannel.TOPIC_PATTERN));
        container.addMessageListener(
                chatAdminAssignedRedisSubscriber,
                new PatternTopic(ChatRedisChannel.ADMIN_ASSIGNED_TOPIC_PATTERN)
        );
        container.addMessageListener(
                chatSessionExpiredRedisSubscriber,
                new PatternTopic(ChatRedisChannel.SESSION_EXPIRED_TOPIC_PATTERN)
        );

        return container;
    }

    private <T> RedisTemplate<String, T> createRedisTemplate(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<T> valueSerializer
    ) {
        RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setHashValueSerializer(valueSerializer);

        return redisTemplate;
    }
}
