package com.example.team3plusspring.global.security.jwt;

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

    private void authenticate(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        Claims claims = jwtTokenProvider.parseClaims(token);
        Authentication authentication = jwtTokenProvider.getAuthentication(claims);

        accessor.setUser(authentication);
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(CHAT_SUBSCRIBE_PREFIX)) {
            return;
        }

        Authentication authentication = getAuthentication(accessor);
        CustomUserDetails userDetails = getUserDetails(authentication);
        Long roomId = getRoomId(destination);

        chatRoomService.validateRoomAccess(roomId, userDetails.getUser());
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

    private String resolveToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
