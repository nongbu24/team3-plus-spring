package com.example.team3plusspring.global.config;

import com.example.team3plusspring.global.security.jwt.StompSubscriptionOutboundInterceptor;
import com.example.team3plusspring.global.security.jwt.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthInterceptor stompAuthInterceptor;
    private final StompSubscriptionOutboundInterceptor stompSubscriptionOutboundInterceptor;

    // 클라이언트는 /pub으로 메시지를 보내고, 서버는 /sub으로 구독자에게 메시지를 전달한다.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    // WebSocket 연결 시작점이다. SockJS를 함께 열어 WebSocket을 바로 쓰기 어려운 환경도 지원한다.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // CONNECT, SUBSCRIBE 같은 클라이언트 입력 프레임이 컨트롤러에 도착하기 전에 인증/권한을 검사한다.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }

    // 구독이 무효화된 세션에는 해당 채팅방 메시지만 내려보내지 않는다.
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompSubscriptionOutboundInterceptor);
    }
}
