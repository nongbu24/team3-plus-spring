package com.example.team3plusspring.global.config;

import com.example.team3plusspring.domain.chat.redis.ChatRedisChannel;
import com.example.team3plusspring.domain.chat.redis.ChatRedisSubscriber;
import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
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
        // Redis Pub/Sub 메시지를 JSON으로 전달해 사람이 읽기 쉽고 서버 버전 변경에도 더 안전하게 처리한다.
        return new JacksonJsonRedisSerializer<>(objectMapper, ChatMessageResponse.class);
    }

    @Bean
    public RedisTemplate<String, ChatMessageResponse> chatRedisTemplate(
            RedisConnectionFactory connectionFactory,
            RedisSerializer<ChatMessageResponse> chatMessageRedisSerializer
    ) {
        RedisTemplate<String, ChatMessageResponse> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(chatMessageRedisSerializer);
        redisTemplate.setHashKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setHashValueSerializer(chatMessageRedisSerializer);

        return redisTemplate;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatRedisSubscriber chatRedisSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(chatRedisSubscriber, new PatternTopic(ChatRedisChannel.TOPIC_PATTERN));

        return container;
    }
}
