package com.example.team3plusspring.domain.chat.port;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class RemovedAdminSubscription {
    private final String sessionId;
    private final String subscriptionId;
}
