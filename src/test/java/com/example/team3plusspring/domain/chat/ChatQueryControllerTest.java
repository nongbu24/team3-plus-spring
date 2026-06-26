package com.example.team3plusspring.domain.chat;

import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.security.jwt.JwtTokenProvider;
import com.example.team3plusspring.support.RedisTestSupport;
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
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void 전체메시지조회_관리자토큰_성공한다() throws Exception {
        // given
        String accessToken = createAdminAccessToken();

        // when & then
        mockMvc.perform(get("/api/chat/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data").isArray());
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

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
