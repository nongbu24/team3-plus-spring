package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.port.RemovedAdminSubscription;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class ChatStompSubscriptionManager {
    private final MessageChannel clientInboundChannel;

    public ChatStompSubscriptionManager(@Lazy @Qualifier("clientInboundChannel") MessageChannel clientInboundChannel) {
        this.clientInboundChannel = clientInboundChannel;
    }

    public void unsubscribeAll(Set<RemovedAdminSubscription> subscriptions) {
        subscriptions.forEach(this::unsubscribe);
    }

    private void unsubscribe(RemovedAdminSubscription subscription) {
        if (!StringUtils.hasText(subscription.getSessionId()) || !StringUtils.hasText(subscription.getSubscriptionId())) {
            return;
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE);
        accessor.setSessionId(subscription.getSessionId());
        accessor.setSubscriptionId(subscription.getSubscriptionId());
        accessor.setLeaveMutable(true);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        clientInboundChannel.send(message);
    }
}
