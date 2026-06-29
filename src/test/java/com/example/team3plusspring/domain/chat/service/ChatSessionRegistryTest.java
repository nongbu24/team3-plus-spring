package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSessionRegistryTest {

    private final ChatSessionRegistry chatSessionRegistry = new ChatSessionRegistry();

    @Test
    void 같은사용자가같은방에여러세션으로입장하면_마지막세션종료시에만_퇴장대상으로반환한다() {
        // given
        chatSessionRegistry.enter("session-1", 1L, 10L);
        chatSessionRegistry.enter("session-2", 1L, 10L);

        // when
        Set<Long> firstDisconnectedRooms = chatSessionRegistry.removeSession("session-1");
        Set<Long> lastDisconnectedRooms = chatSessionRegistry.removeSession("session-2");

        // then
        assertThat(firstDisconnectedRooms).isEmpty();
        assertThat(lastDisconnectedRooms).containsExactly(10L);
    }

    @Test
    void 같은세션에서같은방에반복입장해도_활성세션수는한번만증가한다() {
        // given
        chatSessionRegistry.enter("session-1", 1L, 10L);
        chatSessionRegistry.enter("session-1", 1L, 10L);

        // when
        Set<Long> disconnectedRooms = chatSessionRegistry.removeSession("session-1");

        // then
        assertThat(disconnectedRooms).containsExactly(10L);
    }

    @Test
    void 명시적으로퇴장하면_같은사용자의같은방세션들을퇴장대상에서제거한다() {
        // given
        chatSessionRegistry.enter("session-1", 1L, 10L);
        chatSessionRegistry.enter("session-2", 1L, 10L);

        // when
        Set<String> sessionIds = chatSessionRegistry.leaveAll(1L, 10L);

        // then
        assertThat(sessionIds).containsExactlyInAnyOrder("session-1", "session-2");
        assertThat(chatSessionRegistry.removeSession("session-1")).isEmpty();
        assertThat(chatSessionRegistry.removeSession("session-2")).isEmpty();
    }

    @Test
    void 명시적으로퇴장하면_같은사용자의같은방구독세션도닫기대상으로반환한다() {
        // given
        chatSessionRegistry.subscribe("session-1", 1L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("session-2", 1L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("session-3", 2L, UserRole.USER, 10L);

        // when
        Set<String> sessionIds = chatSessionRegistry.leaveAll(1L, 10L);

        // then
        assertThat(sessionIds).containsExactlyInAnyOrder("session-1", "session-2");
    }

    @Test
    void 담당자가배정되면_같은방의다른관리자세션만제거한다() {
        // given
        chatSessionRegistry.subscribe("assigned-admin", 1L, UserRole.ADMIN, 10L);
        chatSessionRegistry.subscribe("other-admin", 2L, UserRole.ADMIN, 10L);
        chatSessionRegistry.subscribe("customer", 3L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("other-room-admin", 2L, UserRole.ADMIN, 20L);
        chatSessionRegistry.enter("other-admin", 2L, 10L);
        chatSessionRegistry.enter("other-admin", 2L, 20L);

        // when
        Set<String> sessionIds = chatSessionRegistry.removeAdminSessionsExcept(10L, 1L);

        // then
        assertThat(sessionIds).containsExactly("other-admin");
        assertThat(chatSessionRegistry.removeSession("other-admin")).containsExactly(20L);
    }
}
