package com.example.team3plusspring.domain.chat.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
class SessionRoom {
    private final Long userId;
    private final Long roomId;
}
