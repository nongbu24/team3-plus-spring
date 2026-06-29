package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void 입장선점을되돌리면_해당세션의입장정보와활동정보를정리한다() {
        // given
        chatSessionRegistry.enter("session-1", 1L, 10L);

        // when
        chatSessionRegistry.rollbackEnter("session-1", 1L, 10L);

        // then
        assertThat(chatSessionRegistry.isEntered("session-1", 1L, 10L)).isFalse();
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).isEmpty();
    }

    @Test
    void 입장선점을되돌려도_같은방의다른활성세션은유지한다() {
        // given
        chatSessionRegistry.enter("session-1", 1L, 10L);
        chatSessionRegistry.enter("session-2", 1L, 10L);

        // when
        chatSessionRegistry.rollbackEnter("session-1", 1L, 10L);

        // then
        assertThat(chatSessionRegistry.isEntered("session-1", 1L, 10L)).isFalse();
        assertThat(chatSessionRegistry.isEntered("session-2", 1L, 10L)).isTrue();
        assertThat(chatSessionRegistry.findAndMarkWarningRoomIds(java.time.Duration.ZERO)).containsExactly(10L);
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
    void 구독해제하면_같은세션의_해당구독만_무효화한다() {
        // given
        chatSessionRegistry.subscribe("session-1", "sub-1", 1L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("session-1", "sub-2", 1L, UserRole.USER, 20L);

        // when
        chatSessionRegistry.unsubscribe("session-1", "sub-1");

        // then
        assertThat(chatSessionRegistry.isSubscribed("session-1", 10L)).isFalse();
        assertThat(chatSessionRegistry.isSubscribed("session-1", 20L)).isTrue();
    }

    @Test
    void 담당자가배정되면_같은방의다른관리자구독과입장정보만제거한다() {
        // given
        chatSessionRegistry.subscribe("assigned-admin", "sub-1", 1L, UserRole.ADMIN, 10L);
        chatSessionRegistry.subscribe("other-admin", "sub-2", 2L, UserRole.ADMIN, 10L);
        chatSessionRegistry.subscribe("customer", 3L, UserRole.USER, 10L);
        chatSessionRegistry.subscribe("other-room-admin", 2L, UserRole.ADMIN, 20L);
        chatSessionRegistry.enter("other-admin", 2L, 10L);
        chatSessionRegistry.enter("other-admin", 2L, 20L);

        // when
        var removedSubscriptions = chatSessionRegistry.removeAdminSessionsExcept(10L, 1L);

        // then
        assertThat(removedSubscriptions)
                .containsExactly(new ChatSessionRegistry.RemovedAdminSubscription("other-admin", "sub-2"));
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

    @Test
    void 마지막활성세션이제거되면_방활동정보를삭제한다() {
        // given
        ChatActivityStore chatActivityStore = mock(ChatActivityStore.class);
        ChatSessionRegistry registry = new ChatSessionRegistry(chatActivityStore);
        registry.enter("session-1", 1L, 10L);

        // when
        registry.removeSession("session-1");

        // then
        verify(chatActivityStore).removeRoomActivity(10L);
    }

    @Test
    void 같은방에다른활성세션이남아있으면_방활동정보를삭제하지않는다() {
        // given
        ChatActivityStore chatActivityStore = mock(ChatActivityStore.class);
        ChatSessionRegistry registry = new ChatSessionRegistry(chatActivityStore);
        registry.enter("session-1", 1L, 10L);
        registry.enter("session-2", 2L, 10L);

        // when
        registry.removeSession("session-1");

        // then
        verify(chatActivityStore, never()).removeRoomActivity(10L);
    }

    @Test
    void 공유활동정보를보존하는저장소는_로컬마지막세션이종료되어도_방활동정보를삭제하지않는다() {
        // given
        ChatActivityStore chatActivityStore = mock(ChatActivityStore.class);
        ChatSessionRegistry registry = new ChatSessionRegistry(chatActivityStore);
        when(chatActivityStore.preservesSharedRoomActivity()).thenReturn(true);
        registry.enter("session-1", 1L, 10L);

        // when
        registry.removeSession("session-1");

        // then
        verify(chatActivityStore, never()).removeRoomActivity(10L);
    }

    @Test
    void 로컬세션정리는_DB처리없이_해당사용자방세션만제거한다() {
        // given
        ChatActivityStore chatActivityStore = mock(ChatActivityStore.class);
        ChatSessionRegistry registry = new ChatSessionRegistry(chatActivityStore);
        registry.subscribe("session-1", 1L, UserRole.USER, 10L);
        registry.enter("session-1", 1L, 10L);
        registry.subscribe("session-2", 2L, UserRole.USER, 10L);
        registry.enter("session-2", 2L, 10L);

        // when
        registry.removeLocalSessions(1L, 10L);

        // then
        assertThat(registry.canSend("session-1", 1L, 10L)).isFalse();
        assertThat(registry.canSend("session-2", 2L, 10L)).isTrue();
    }
}
