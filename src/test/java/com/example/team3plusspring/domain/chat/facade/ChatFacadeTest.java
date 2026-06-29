package com.example.team3plusspring.domain.chat.facade;

import com.example.team3plusspring.domain.chat.dto.ChatMessageResponse;
import com.example.team3plusspring.domain.chat.entity.ChatMember;
import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.repository.ChatMemberRepository;
import com.example.team3plusspring.domain.chat.repository.ChatMessageRepository;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.chat.service.ChatAdminSessionService;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatFacadeTest {

    @Mock
    ChatMessageRepository chatMessageRepository;

    @Mock
    ChatRoomRepository chatRoomRepository;

    @Mock
    ChatMemberRepository chatMemberRepository;

    @Mock
    ChatAdminSessionService chatAdminSessionService;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ChatFacade chatFacade;

    @Test
    void 입장_배정전관리자이면_채팅참여자로저장하지않는다() {
        // given
        User customer = user(1L);
        User admin = admin(2L);
        ChatRoom room = room(10L, customer);

        given(chatRoomRepository.findByIdWithLock(room.getId())).willReturn(Optional.of(room));
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 100L);

            return message;
        });

        // when
        ChatMessageResponse response = chatFacade.enterRoom(room.getId(), admin);

        // then
        assertThat(response.getContent()).isEqualTo("관리자님이 입장했습니다");
        verify(chatMemberRepository, never()).findByChatRoomIdAndUserId(room.getId(), admin.getId());
        verify(chatMemberRepository, never()).save(any(ChatMember.class));
    }

    @Test
    void 자동만료퇴장_이미퇴장한회원이면_퇴장메시지를저장하지않는다() {
        // given
        User user = user(1L);
        ChatRoom room = room(10L, user);
        ChatMember leftMember = ChatMember.join(room, user);
        leftMember.leave();

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(chatRoomRepository.findByIdWithLock(room.getId())).willReturn(Optional.of(room));
        given(chatMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
                .willReturn(Optional.of(leftMember));

        // when
        Optional<ChatMessageResponse> response = chatFacade.leaveInactiveRoom(room.getId(), user.getId());

        // then
        assertThat(response).isEmpty();
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void 자동만료퇴장_입장중인회원이면_퇴장메시지를한번저장한다() {
        // given
        User user = user(1L);
        ChatRoom room = room(10L, user);
        ChatMember joinedMember = ChatMember.join(room, user);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(chatRoomRepository.findByIdWithLock(room.getId())).willReturn(Optional.of(room));
        given(chatMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
                .willReturn(Optional.of(joinedMember));
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 100L);

            return message;
        });

        // when
        Optional<ChatMessageResponse> response = chatFacade.leaveInactiveRoom(room.getId(), user.getId());

        // then
        assertThat(response).isPresent();
        assertThat(joinedMember.getLeftAt()).isNotNull();

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("홍길동님이 퇴장했습니다");
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

    private ChatRoom room(Long id, User user) {
        ChatRoom room = ChatRoom.create(user);
        ReflectionTestUtils.setField(room, "id", id);

        return room;
    }
}
