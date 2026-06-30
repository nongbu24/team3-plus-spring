package com.example.team3plusspring.domain.chat;

import com.example.team3plusspring.domain.chat.entity.ChatMessage;
import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.repository.ChatMessageRepository;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.security.jwt.JwtTokenProvider;
import com.example.team3plusspring.support.RedisTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatQueryControllerTest extends RedisTestSupport {
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
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        chatMessageRepository.deleteAllInBatch();
        chatRoomRepository.deleteAllInBatch();
    }

    @Test
    void 전체메시지조회_최근대화방기준으로_메시지를묶어서반환한다() throws Exception {
        // given
        String accessToken = createAdminAccessToken();
        User customer1 = saveUser("고객1");
        User customer2 = saveUser("고객2");
        ChatRoom room1 = chatRoomRepository.save(ChatRoom.create(customer1));
        ChatRoom room2 = chatRoomRepository.save(ChatRoom.create(customer2));

        chatMessageRepository.save(ChatMessage.create(customer1.getId(), customer1.getName(), room1, "방1 첫 메시지"));
        chatMessageRepository.save(ChatMessage.create(customer2.getId(), customer2.getName(), room2, "방2 메시지"));
        chatMessageRepository.save(ChatMessage.create(customer1.getId(), customer1.getName(), room1, "방1 최신 메시지"));

        // when & then
        mockMvc.perform(get("/api/chat/messages")
                        .param("size", "3")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roomId").value(room1.getId()))
                .andExpect(jsonPath("$.data[0].messages[0].content").value("방1 최신 메시지"))
                .andExpect(jsonPath("$.data[0].messages[1].content").value("방1 첫 메시지"))
                .andExpect(jsonPath("$.data[1].roomId").value(room2.getId()))
                .andExpect(jsonPath("$.data[1].messages[0].content").value("방2 메시지"));
    }

    @Test
    void 전체메시지조회_일반사용자토큰_실패한다() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "Password123");
        String accessToken = loginAccessToken(email, "Password123");

        // when & then
        mockMvc.perform(get("/api/chat/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.FORBIDDEN.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.name()));
    }

    @Test
    void 전체메시지조회_size가최대값을초과하면_검증에실패한다() throws Exception {
        // given
        String accessToken = createAdminAccessToken();

        // when & then
        mockMvc.perform(get("/api/chat/messages")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.VALIDATION_FAILED.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()));
    }

    @Test
    void 이후메시지조회_size가500이면_검증을통과한다() throws Exception {
        // given
        User customer = saveUser("홍길동");
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(customer));
        String accessToken = jwtTokenProvider.createAccessToken(customer.getId());

        // when & then
        mockMvc.perform(get("/api/chat/rooms/{roomId}/messages/after/{lastMessageId}", room.getId(), 0L)
                        .param("size", "500")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data").isArray());
    }

    private void signup(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "name": "홍길동",
                                  "phone": "010-1234-5678"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private String loginAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private String createAdminAccessToken() {
        User admin = User.create(
                uniqueEmail(),
                passwordEncoder.encode("Password123"),
                "관리자",
                "010-0000-0001"
        );
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);
        User savedAdmin = userRepository.save(admin);

        return jwtTokenProvider.createAccessToken(savedAdmin.getId());
    }

    private User saveUser(String name) {
        return userRepository.save(User.create(
                uniqueEmail(),
                passwordEncoder.encode("Password123"),
                name,
                "010-1234-5678"
        ));
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
