package com.example.team3plusspring.global.security.jwt;

import com.example.team3plusspring.domain.chat.service.ChatRoomService;
import com.example.team3plusspring.domain.chat.service.ChatSessionRegistry;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthInterceptorTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    ChatRoomService chatRoomService;

    @Mock
    ChatSessionRegistry chatSessionRegistry;

    @InjectMocks
    StompAuthInterceptor stompAuthInterceptor;

    @Test
    void 커넥트_유효한토큰이면_사용자인증정보를저장한다() {
        // given
        String token = "valid-token";
        Claims claims = mock(Claims.class);
        Authentication authentication = authentication();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = message(accessor);

        when(jwtTokenProvider.parseClaims(token)).thenReturn(claims);
        when(jwtTokenProvider.getAuthentication(claims)).thenReturn(authentication);

        // when
        Message<?> result = stompAuthInterceptor.preSend(message, null);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        // then
        assertThat(resultAccessor.getUser()).isEqualTo(authentication);
    }

    @Test
    void 커넥트_토큰이없으면_실패한다() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = message(accessor);

        // when & then
        assertThatThrownBy(() -> stompAuthInterceptor.preSend(message, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 구독_채팅방목적지이면_채팅방접근권한을검증한다() {
        // given
        Authentication authentication = authentication();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/1");
        accessor.setSessionId("session-1");
        accessor.setUser(authentication);
        Message<byte[]> message = message(accessor);

        // when
        stompAuthInterceptor.preSend(message, null);

        // then
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        verify(chatRoomService).validateRoomAccess(1L, userDetails.getUser());
        verify(chatSessionRegistry).subscribe("session-1", userDetails.getUser().getId(), UserRole.USER, 1L);
    }

    @Test
    void 구독_인증정보가없으면_실패한다() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/1");
        Message<byte[]> message = message(accessor);

        // when & then
        assertThatThrownBy(() -> stompAuthInterceptor.preSend(message, null))
                .isInstanceOf(BusinessException.class);
    }

    private Authentication authentication() {
        User user = User.create("user@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(user, "id", 1L);
        CustomUserDetails userDetails = new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
