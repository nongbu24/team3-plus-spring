package com.example.team3plusspring.global.security.jwt;

import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import com.example.team3plusspring.domain.chat.service.ChatRoomService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CHAT_SUBSCRIBE_PREFIX = "/sub/chat/";

    private final JwtTokenProvider jwtTokenProvider;
    private final ChatRoomService chatRoomService;
    private final ChatSessionRegistry chatSessionRegistry;

    /**
     * STOMP 프레임이 서버로 들어올 때마다 호출되는 진입점이다.
     * CONNECT에서는 JWT 인증을 만들고, SUBSCRIBE에서는 채팅방 구독 권한을 확인한다.
     *
     * @param message 클라이언트가 보낸 STOMP 메시지로, 명령(CONNECT, SUBSCRIBE 등), 헤더, 본문을 담고 있다.
     * @param channel 메시지가 지나가는 Spring Messaging 채널이다. 현재 로직에서는 직접 사용하지 않는다.
     * @return 인증/권한 검사를 통과해 계속 처리할 메시지
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscription(accessor);
        }

        return message;
    }

    // WebSocket은 HTTP 필터를 계속 타지 않으므로, STOMP CONNECT 헤더의 JWT로 인증 객체를 직접 심는다.
    private void authenticate(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        Claims claims = jwtTokenProvider.parseClaims(token);
        Authentication authentication = jwtTokenProvider.getAuthentication(claims);

        accessor.setUser(authentication);
    }

    // /sub/chat/{roomId} 구독은 메시지 수신 권한과 같아서, 방 접근 권한을 구독 시점에 막는다.
    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(CHAT_SUBSCRIBE_PREFIX)) {
            return;
        }

        Authentication authentication = getAuthentication(accessor);
        CustomUserDetails userDetails = getUserDetails(authentication);
        Long roomId = getRoomId(destination);

        chatRoomService.validateRoomAccess(roomId, userDetails.getUser());
        chatSessionRegistry.subscribe(
                getSessionId(accessor),
                userDetails.getUser().getId(),
                userDetails.getUser().getRole(),
                roomId
        );
    }

    private Authentication getAuthentication(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication) {
            return authentication;
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private CustomUserDetails getUserDetails(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private Long getRoomId(String destination) {
        try {
            return Long.valueOf(destination.substring(CHAT_SUBSCRIBE_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND);
        }
    }

    private String getSessionId(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();

        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return sessionId;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
