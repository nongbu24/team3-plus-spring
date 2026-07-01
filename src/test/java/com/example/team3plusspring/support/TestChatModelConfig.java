package com.example.team3plusspring.support;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TestChatModelConfig {

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel testChatModel() {
        return prompt -> ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("테스트 AI 응답입니다."))))
                .build();
    }
}
