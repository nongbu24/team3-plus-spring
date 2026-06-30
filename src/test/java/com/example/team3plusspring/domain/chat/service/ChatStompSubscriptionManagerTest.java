package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.port.RemovedAdminSubscription;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ChatStompSubscriptionManagerTest {

    private final MessageChannel clientInboundChannel = mock(MessageChannel.class);
    private final ChatStompSubscriptionManager manager = new ChatStompSubscriptionManager(clientInboundChannel);

    @Test
    void 제거된구독은_세션을닫지않고_UNSUBSCRIBE_프레임으로해제한다() {
        // given
        RemovedAdminSubscription subscription = new RemovedAdminSubscription("session-1", "sub-1");

        // when
        manager.unsubscribeAll(Set.of(subscription));

        // then
        ArgumentCaptor<Message<byte[]>> captor = ArgumentCaptor.captor();
        verify(clientInboundChannel).send(captor.capture());

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(captor.getValue());
        assertThat(accessor.getCommand()).isEqualTo(StompCommand.UNSUBSCRIBE);
        assertThat(accessor.getSessionId()).isEqualTo("session-1");
        assertThat(accessor.getSubscriptionId()).isEqualTo("sub-1");
    }

    @Test
    void 구독아이디가없으면_브로커에해제프레임을보내지않는다() {
        // given
        RemovedAdminSubscription subscription = new RemovedAdminSubscription("session-1", null);

        // when
        manager.unsubscribeAll(Set.of(subscription));

        // then
        verify(clientInboundChannel, never()).send(any());
    }
}
