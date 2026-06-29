package com.example.team3plusspring.domain.chat;

import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.security.jwt.JwtTokenProvider;
import com.example.team3plusspring.support.RedisTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatRoomControllerTest extends RedisTestSupport {
    private static final String PASSWORD = "Password123";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void 채팅방목록조회_최신순으로_페이지조회한다() throws Exception {
        // given
        User customer = saveUser("홍길동");
        User otherCustomer = saveUser("다른회원");
        ChatRoom oldRoom = chatRoomRepository.save(ChatRoom.create(customer));
        chatRoomRepository.save(ChatRoom.create(otherCustomer));
        Thread.sleep(5);
        ChatRoom newRoom = chatRoomRepository.save(ChatRoom.create(customer));
        String accessToken = jwtTokenProvider.createAccessToken(customer.getId());

        // when & then
        mockMvc.perform(get("/api/chat/rooms")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].roomId").value(newRoom.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.number").value(0));

        mockMvc.perform(get("/api/chat/rooms")
                        .param("page", "1")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].roomId").value(oldRoom.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.number").value(1));
    }

    private User saveUser(String name) {
        return userRepository.save(User.create(
                uniqueEmail(),
                passwordEncoder.encode(PASSWORD),
                name,
                "010-1234-5678"
        ));
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
