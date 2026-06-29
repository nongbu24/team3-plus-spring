package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSessionRegistryTest {

    private final ChatSessionRegistry chatSessionRegistry = new ChatSessionRegistry();

    @Test
    void 같은사용자가같은방에여러세션으로입장하면_마지막세션종료시에만_활동정보를정리한다() {
        // given
        chatSessionRegistry.enter("session-1", 1L, 10L);
        chatSessionRegistry.enter("session-2", 1L, 10L);

        // when
        chatSessionRegistry.removeSession("session-1");

        // then
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).containsExactly(10L);

        // when
        chatSessionRegistry.removeSession("session-2");

        // then
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).isEmpty();
    }

    @Test
    void 같은세션에서같은방에반복입장해도_활성세션수는한번만증가한다() {
        // given
        boolean firstEntered = chatSessionRegistry.enter("session-1", 1L, 10L);
        boolean secondEntered = chatSessionRegistry.enter("session-1", 1L, 10L);

        // when
        chatSessionRegistry.removeSession("session-1");

        // then
        assertThat(firstEntered).isTrue();
        assertThat(secondEntered).isFalse();
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).isEmpty();
    }

    @Test
    void 명시적으로퇴장하면_같은사용자의같은방입장정보를제거한다() {
        // given
        chatSessionRegistry.enter("session-1", 1L, 10L);
        chatSessionRegistry.enter("session-2", 1L, 10L);

        // when
        chatSessionRegistry.leaveAll(1L, 10L);

        // then
        assertThat(chatSessionRegistry.isEntered("session-1", 1L, 10L)).isFalse();
        assertThat(chatSessionRegistry.isEntered("session-2", 1L, 10L)).isFalse();
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).isEmpty();
    }

    @Test
    void 명시적으로퇴장하면_같은사용자의같은방구독을무효화한다() {
        // given
        chatSessionRegistry.subscribe("session-1", 1L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("session-2", 1L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("session-3", 2L, UserRole.USER, 10L);

        // when
        chatSessionRegistry.leaveAll(1L, 10L);

        // then
        assertThat(chatSessionRegistry.isSubscribed("session-1", 10L)).isFalse();
        assertThat(chatSessionRegistry.isSubscribed("session-2", 10L)).isFalse();
        assertThat(chatSessionRegistry.isSubscribed("session-3", 10L)).isTrue();
    }

    @Test
    void 담당자가배정되면_같은방의다른관리자구독과입장정보만제거한다() {
        // given
        chatSessionRegistry.subscribe("assigned-admin", 1L, UserRole.ADMIN, 10L);
        chatSessionRegistry.subscribe("other-admin", 2L, UserRole.ADMIN, 10L);
        chatSessionRegistry.subscribe("customer", 3L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("other-room-admin", 2L, UserRole.ADMIN, 20L);
        chatSessionRegistry.enter("other-admin", 2L, 10L);
        chatSessionRegistry.enter("other-admin", 2L, 20L);

        // when
        chatSessionRegistry.removeAdminSessionsExcept(10L, 1L);

        // then
        assertThat(chatSessionRegistry.isSubscribed("assigned-admin", 10L)).isTrue();
        assertThat(chatSessionRegistry.isSubscribed("other-admin", 10L)).isFalse();
        assertThat(chatSessionRegistry.isSubscribed("customer", 10L)).isTrue();
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).containsExactly(20L);
        chatSessionRegistry.removeSession("other-admin");
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).isEmpty();
    }

    @Test
    void 메시지는_같은세션이_구독과입장을모두완료한방에만_보낼수있다() {
        // given
        chatSessionRegistry.subscribe("session-1", 1L, UserRole.USER, 10L);

        // when & then
        assertThat(chatSessionRegistry.canSend("session-1", 1L, 10L)).isFalse();

        // when
        chatSessionRegistry.enter("session-1", 1L, 10L);

        // then
        assertThat(chatSessionRegistry.canSend("session-1", 1L, 10L)).isTrue();
    }
}
