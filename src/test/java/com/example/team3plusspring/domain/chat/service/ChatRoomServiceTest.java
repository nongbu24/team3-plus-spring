package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.domain.chat.dto.ChatRoomStatusRequest;
import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import com.example.team3plusspring.domain.chat.port.ChatSessionExpiredEventPublisher;
import com.example.team3plusspring.domain.chat.port.RemovedSubscription;
import com.example.team3plusspring.domain.chat.repository.ChatMemberRepository;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    ChatRoomRepository chatRoomRepository;

    @Mock
    ChatMemberRepository chatMemberRepository;

    @Mock
    ChatAdminSessionService chatAdminSessionService;

    @Mock
    ChatSessionRegistry chatSessionRegistry;

    @Mock
    ChatStompSubscriptionManager chatStompSubscriptionManager;

    @Mock
    ChatSessionExpiredEventPublisher chatSessionExpiredEventPublisher;

    @Test
    void 상태를완료로변경하면_커밋후_고객과담당관리자세션을정리한다() {
        // given
        User customer = user(1L);
        User admin = admin(2L);
        ChatRoom room = inProgressRoom(10L, customer, admin);
        ChatRoomStatusRequest request = ChatRoomStatusRequest.of(ChatStatus.COMPLETED);
        Set<RemovedSubscription> customerSubscriptions = Set.of(new RemovedSubscription("session-1", "sub-1"));
        Set<RemovedSubscription> adminSubscriptions = Set.of(new RemovedSubscription("session-2", "sub-2"));
        ChatRoomService chatRoomService = new ChatRoomService(
                chatRoomRepository,
                chatMemberRepository,
                chatAdminSessionService,
                chatSessionRegistry,
                chatStompSubscriptionManager,
                chatSessionExpiredEventPublisher
        );

        given(chatRoomRepository.findByIdWithLock(room.getId())).willReturn(Optional.of(room));
        given(chatSessionRegistry.removeLocalSessions(customer.getId(), room.getId()))
                .willReturn(customerSubscriptions);
        given(chatSessionRegistry.removeLocalSessions(admin.getId(), room.getId()))
                .willReturn(adminSubscriptions);

        TransactionSynchronizationManager.initSynchronization();
        try {
            // when
            chatRoomService.updateStatus(room.getId(), admin, request);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        // then
        verify(chatSessionRegistry).removeLocalSessions(customer.getId(), room.getId());
        verify(chatSessionRegistry).removeLocalSessions(admin.getId(), room.getId());
        verify(chatStompSubscriptionManager).unsubscribeAll(customerSubscriptions);
        verify(chatStompSubscriptionManager).unsubscribeAll(adminSubscriptions);
        verify(chatSessionExpiredEventPublisher).publish(room.getId(), customer.getId());
        verify(chatSessionExpiredEventPublisher).publish(room.getId(), admin.getId());
    }

    private ChatRoom inProgressRoom(Long id, User customer, User admin) {
        ChatRoom room = ChatRoom.create(customer);
        ReflectionTestUtils.setField(room, "id", id);
        room.assignAdmin(admin);
        room.changeTo(ChatStatus.IN_PROGRESS);

        return room;
    }

    private User user(Long id) {
        User user = User.create("user@example.com", "Password123", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    private User admin(Long id) {
        User admin = User.create("admin@example.com", "Password123", "관리자", "010-1234-5678");
        ReflectionTestUtils.setField(admin, "id", id);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        return admin;
    }
}
