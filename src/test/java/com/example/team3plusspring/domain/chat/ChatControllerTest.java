package com.example.team3plusspring.domain.chat;

import com.example.team3plusspring.domain.chat.dto.ChatMessageRequest;
import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.entity.ChatStatus;
import com.example.team3plusspring.domain.chat.facade.ChatFacade;
import com.example.team3plusspring.domain.chat.repository.ChatMemberRepository;
import com.example.team3plusspring.domain.chat.repository.ChatMessageRepository;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.chat.service.ChatRoomService;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {
    private static final int BODY_STATUS = 200;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    ChatMemberRepository chatMemberRepository;

    @Autowired
    ChatFacade chatFacade;

    @Autowired
    ChatRoomService chatRoomService;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAllInBatch();
        chatMemberRepository.deleteAllInBatch();
        chatRoomRepository.deleteAllInBatch();
    }

    @Test
    void 채팅방생성_일반회원이면_대기상태채팅방을생성한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        String accessToken = accessToken(user);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/chat/rooms/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.roomId").isNumber())
                .andExpect(jsonPath("$.data.name").value("홍길동님의 1:1 문의"))
                .andExpect(jsonPath("$.data.customerId").value(user.getId()))
                .andExpect(jsonPath("$.data.customerName").value("홍길동"))
                .andExpect(jsonPath("$.data.adminId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value(ChatStatus.WAITING.name()))
                .andReturn();

        Number roomId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.roomId");

        assertThat(chatRoomRepository.findById(roomId.longValue())).isPresent();
        assertThat(hasActiveMember(roomId.longValue(), user.getId())).isTrue();
    }

    @Test
    void 채팅방생성_관리자이면_실패한다() throws Exception {
        // given
        User admin = saveAdmin("관리자");
        String accessToken = accessToken(admin);

        // when & then
        mockMvc.perform(post("/api/chat/rooms/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.FORBIDDEN.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
    }

    @Test
    void 채팅방목록조회_일반회원이면_본인채팅방만조회한다() throws Exception {
        // given
        User owner = saveUser("홍길동");
        User other = saveUser("김철수");
        ChatRoom ownerRoom = chatRoomRepository.save(ChatRoom.create(owner));
        chatRoomRepository.save(ChatRoom.create(other));
        String accessToken = accessToken(owner);

        // when & then
        mockMvc.perform(get("/api/chat/rooms")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].roomId").value(ownerRoom.getId()))
                .andExpect(jsonPath("$.data.content[0].customerId").value(owner.getId()))
                .andExpect(jsonPath("$.data.pageable").doesNotExist())
                .andExpect(jsonPath("$.data.sort").doesNotExist());
    }

    @Test
    void 채팅방단건조회_본인채팅방이면_성공한다() throws Exception {
        // given
        User customer = saveUser("홍길동");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(customer));
        String accessToken = accessToken(customer);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}", room.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.roomId").value(room.getId()))
                .andExpect(jsonPath("$.data.name").value("홍길동님의 1:1 문의"))
                .andExpect(jsonPath("$.data.customerId").value(customer.getId()))
                .andExpect(jsonPath("$.data.customerName").value("홍길동"));
    }

    @Test
    void 채팅방목록조회_관리자면_상태로전체채팅방을필터링한다() throws Exception {
        // given
        User waitingCustomer = saveUser("대기고객");
        User completedCustomer = saveUser("완료고객");
        User admin = saveAdmin("관리자");
        ChatRoom waitingRoom = chatRoomRepository.save(ChatRoom.create(waitingCustomer));
        ChatRoom completedRoom = chatRoomRepository.save(ChatRoom.create(completedCustomer));
        completedRoom.assignAdmin(admin);
        completedRoom.changeTo(ChatStatus.IN_PROGRESS);
        completedRoom.changeTo(ChatStatus.COMPLETED);
        chatRoomRepository.save(completedRoom);
        String accessToken = accessToken(admin);

        // when & then
        mockMvc.perform(get("/api/chat/rooms")
                        .param("status", "WAITING")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].roomId").value(waitingRoom.getId()))
                .andExpect(jsonPath("$.data.content[0].status").value(ChatStatus.WAITING.name()));
    }

    @Test
    void 채팅방목록조회_잘못된상태값이면_INVALID_ENUM_VALUE를반환한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        String accessToken = accessToken(user);

        // when & then
        mockMvc.perform(get("/api/chat/rooms")
                        .param("status", "UNKNOWN")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_ENUM_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ENUM_VALUE.name()));
    }

    @Test
    void 문의상태변경_관리자이면_담당자로배정하고상태를변경한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        User admin = saveAdmin("관리자");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        String accessToken = accessToken(admin);

        // when & then
        mockMvc.perform(patch("/api/chat/rooms/{roomId}/status", room.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.roomId").value(room.getId()))
                .andExpect(jsonPath("$.data.adminId").value(admin.getId()))
                .andExpect(jsonPath("$.data.adminName").value("관리자"))
                .andExpect(jsonPath("$.data.status").value(ChatStatus.IN_PROGRESS.name()));

        ChatRoom updatedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();

        assertThat(updatedRoom.getAdminId()).isEqualTo(admin.getId());
        assertThat(updatedRoom.getStatus()).isEqualTo(ChatStatus.IN_PROGRESS);
        assertThat(hasActiveMember(room.getId(), admin.getId())).isTrue();
    }

    @Test
    void 문의상태변경_대기에서완료로변경하면_담당자를배정하지않고실패한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        User admin = saveAdmin("관리자");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        String accessToken = accessToken(admin);

        // when & then
        mockMvc.perform(patch("/api/chat/rooms/{roomId}/status", room.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CHAT_STATUS_TRANSITION.name()));

        ChatRoom updatedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();

        assertThat(updatedRoom.getAdminId()).isNull();
        assertThat(hasActiveMember(room.getId(), admin.getId())).isFalse();
    }

    @Test
    void 문의상태변경_일반회원이면_실패한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        String accessToken = accessToken(user);

        // when & then
        mockMvc.perform(patch("/api/chat/rooms/{roomId}/status", room.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.FORBIDDEN.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
    }

    @Test
    void 문의상태변경_잘못된상태값이면_INVALID_ENUM_VALUE를반환한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        User admin = saveAdmin("관리자");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        String accessToken = accessToken(admin);

        // when & then
        mockMvc.perform(patch("/api/chat/rooms/{roomId}/status", room.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "UNKNOWN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_ENUM_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ENUM_VALUE.name()));
    }

    @Test
    void 메시지조회_채팅방고객이면_최근메시지를최신순으로조회한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        ChatMessage firstMessage = chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "첫 번째 메시지"));
        ChatMessage secondMessage = chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "두 번째 메시지"));
        String accessToken = accessToken(user);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].messageId").value(secondMessage.getId()))
                .andExpect(jsonPath("$.data[0].content").value("두 번째 메시지"))
                .andExpect(jsonPath("$.data[1].messageId").value(firstMessage.getId()))
                .andExpect(jsonPath("$.data[1].content").value("첫 번째 메시지"));
    }

    @Test
    void 메시지조회_미배정채팅방을관리자가조회해도_담당자로배정하지않는다() throws Exception {
        // given
        User user = saveUser("홍길동");
        User admin = saveAdmin("관리자");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "문의 메시지"));
        String accessToken = accessToken(admin);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS));

        ChatRoom updatedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();

        assertThat(updatedRoom.getAdminId()).isNull();
        assertThat(hasActiveMember(room.getId(), admin.getId())).isFalse();
    }

    @Test
    void 구독검증_미배정채팅방을관리자가구독해도_담당자로배정하지않는다() {
        // given
        User user = saveUser("홍길동");
        User admin = saveAdmin("관리자");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));

        // when
        chatRoomService.validateRoomAccess(room.getId(), admin);

        // then
        ChatRoom updatedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();
        assertThat(updatedRoom.getAdminId()).isNull();
        assertThat(updatedRoom.getAdminName()).isNull();
        assertThat(hasActiveMember(room.getId(), admin.getId())).isFalse();
    }

    @Test
    void 구독검증_이미담당자가배정된채팅방을다른관리자가구독하면_실패한다() {
        // given
        User user = saveUser("홍길동");
        User firstAdmin = saveAdmin("첫번째관리자");
        User secondAdmin = saveAdmin("두번째관리자");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        chatFacade.sendMessage(ChatMessageRequest.of(room.getId(), "문의 확인했습니다."), firstAdmin);

        // when & then
        assertThatThrownBy(() -> chatRoomService.validateRoomAccess(room.getId(), secondAdmin))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }

    @Test
    void 메시지조회_다른회원채팅방이면_실패한다() throws Exception {
        // given
        User owner = saveUser("홍길동");
        User other = saveUser("김철수");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(owner));
        String accessToken = accessToken(other);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.CHAT_ROOM_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.CHAT_ROOM_ACCESS_DENIED.name()));
    }

    @Test
    void 이전메시지조회_기준메시지보다작은메시지만조회한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        ChatMessage firstMessage = chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "첫 번째 메시지"));
        ChatMessage secondMessage = chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "두 번째 메시지"));
        chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "세 번째 메시지"));
        String accessToken = accessToken(user);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages/before/{lastMessageId}", room.getId(), secondMessage.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].messageId").value(firstMessage.getId()))
                .andExpect(jsonPath("$.data[0].content").value("첫 번째 메시지"));
    }

    @Test
    void 이후메시지조회_마지막수신메시지보다큰메시지를오래된순으로조회한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        ChatMessage firstMessage = chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "첫 번째 메시지"));
        ChatMessage secondMessage = chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "두 번째 메시지"));
        ChatMessage thirdMessage = chatMessageRepository.save(ChatMessage.create(user.getId(), user.getName(), room, "세 번째 메시지"));
        String accessToken = accessToken(user);

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages/after/{lastReceivedMessageId}", room.getId(), firstMessage.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].messageId").value(secondMessage.getId()))
                .andExpect(jsonPath("$.data[0].content").value("두 번째 메시지"))
                .andExpect(jsonPath("$.data[1].messageId").value(thirdMessage.getId()))
                .andExpect(jsonPath("$.data[1].content").value("세 번째 메시지"));
    }

    @Test
    void 전체메시지조회_일반회원이면_실패한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        String accessToken = accessToken(user);

        // when & then
        mockMvc.perform(get("/api/chat/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.FORBIDDEN.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
    }

    @Test
    void 완료된채팅방이면_메시지와입퇴장메시지를_저장하지않는다() {
        // given
        User user = saveUser("홍길동");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        room.changeTo(ChatStatus.IN_PROGRESS);
        room.changeTo(ChatStatus.COMPLETED);
        chatRoomRepository.save(room);

        ChatMessageRequest request = ChatMessageRequest.of(room.getId(), "끝난 문의에 보내는 메시지");

        // when & then
        assertThatThrownBy(() -> chatFacade.sendMessage(request, user))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_COMPLETED);

        assertThatThrownBy(() -> chatFacade.enterRoom(room.getId(), user))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_COMPLETED);

        assertThatThrownBy(() -> chatFacade.leaveRoom(room.getId(), user))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_ROOM_ALREADY_COMPLETED);

        assertThat(chatMessageRepository.count()).isZero();
    }

    @Test
    void 관리자가_대기중인채팅방에_메시지를보내면_담당자로배정되고처리중으로변경된다() {
        // given
        User user = saveUser("홍길동");
        User admin = saveAdmin("관리자");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(user));
        ChatMessageRequest request = ChatMessageRequest.of(room.getId(), "문의 확인했습니다.");

        // when
        chatFacade.sendMessage(request, admin);

        // then
        ChatRoom updatedRoom = chatRoomRepository.findById(room.getId()).orElseThrow();
        assertThat(updatedRoom.getAdminId()).isEqualTo(admin.getId());
        assertThat(updatedRoom.getAdminName()).isEqualTo("관리자");
        assertThat(updatedRoom.getStatus()).isEqualTo(ChatStatus.IN_PROGRESS);
        assertThat(hasActiveMember(room.getId(), admin.getId())).isTrue();
    }

    private User saveUser(String name) {
        return userRepository.save(User.create(uniqueEmail(), "password", name, "010-1234-5678"));
    }

    private User saveAdmin(String name) {
        User admin = User.create(uniqueEmail(), "password", name, "010-0000-0000");
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        return userRepository.save(admin);
    }

    private String accessToken(User user) {
        return jwtTokenProvider.createAccessToken(user.getId());
    }

    private boolean hasActiveMember(Long roomId, Long userId) {
        return chatMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .filter(member -> member.getLeftAt() == null)
                .isPresent();
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
