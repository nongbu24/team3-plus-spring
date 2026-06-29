package com.example.team3plusspring.global.config;

import com.example.team3plusspring.domain.chat.service.WebSocketSessionStore;
import com.example.team3plusspring.global.security.jwt.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthInterceptor stompAuthInterceptor;
    private final WebSocketSessionStore webSocketSessionStore;

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

    // 서버가 특정 채팅방의 기존 구독 세션을 실제로 끊을 수 있도록 WebSocketSession을 보관한다.
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                webSocketSessionStore.add(session.getId(), session);
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                webSocketSessionStore.remove(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }
        });
    }
}
