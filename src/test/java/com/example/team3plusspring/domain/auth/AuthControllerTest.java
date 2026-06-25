package com.example.team3plusspring.domain.auth;

import com.example.team3plusspring.domain.cart.repository.CartRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.support.RedisTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest extends RedisTestContainerSupport {
    private static final int BODY_STATUS = 200;
    private static final int CREATED_STATUS = 201;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void 회원가입_정상요청_회원과장바구니를생성하고비밀번호를암호화한다() throws Exception {
        // given
        String email = uniqueEmail();
        String rawPassword = "Password123";

        // when & then
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "name": "홍길동",
                                  "phone": "010-1234-5678"
                                }
                                """.formatted(email, rawPassword)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(CREATED_STATUS))
                .andExpect(jsonPath("$.message").value("요청이 성공했습니다."))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn();

        assertResponseKeyOrder(
                result,
                "\"status\"",
                "\"message\"",
                "\"data\""
        );

        User user = userRepository.findByEmail(email).orElseThrow();

        assertThat(user.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, user.getPassword())).isTrue();
        assertThat(cartRepository.existsByUserId(user.getId())).isTrue();
    }

    @Test
    void 회원가입_중복이메일_실패한다() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "Password123");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123",
                                  "name": "홍길동",
                                  "phone": "010-1234-5678"
                                }
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(ErrorCode.EMAIL_ALREADY_EXISTS.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.name()));
    }

    @Test
    void 회원가입_비밀번호에숫자가없으면_실패한다() throws Exception {
        // given
        String email = uniqueEmail();

        // when & then
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "weak",
                                  "name": "홍길동",
                                  "phone": "010-1234-5678"
                                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.VALIDATION_FAILED.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()))
                .andReturn();

        assertResponseKeyOrder(
                result,
                "\"status\"",
                "\"code\"",
                "\"message\"",
                "\"data\""
        );
    }

    @Test
    void 회원가입_전화번호형식이올바르지않으면_실패한다() throws Exception {
        // given
        String email = uniqueEmail();

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123",
                                  "name": "홍길동",
                                  "phone": "invalid-phone"
                                }
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.VALIDATION_FAILED.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED.name()));
    }

    @Test
    void 로그인_정상자격증명_토큰과회원요약을반환한다() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "Password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(jsonPath("$.data.user.userId").isNumber())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.name").value("홍길동"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void 로그인_비밀번호불일치_실패한다() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "Password123");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "WrongPassword123"
                                }
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_LOGIN_CREDENTIALS.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_LOGIN_CREDENTIALS.name()));
    }

    @Test
    void 내정보조회_유효한토큰_회원정보를반환한다() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "Password123");
        String accessToken = loginAccessToken(email, "Password123");

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void 인증필요API_토큰없음_실패한다() throws Exception {
        // when & then
        MvcResult result = mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorCode.UNAUTHORIZED.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.name()))
                .andReturn();

        assertResponseKeyOrder(
                result,
                "\"status\"",
                "\"code\"",
                "\"message\""
        );
    }

    @Test
    void 회원탈퇴_유효한토큰_deletedAt을저장하고기존토큰을거부한다() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "Password123");
        String accessToken = loginAccessToken(email, "Password123");

        // when & then
        mockMvc.perform(post("/api/users/delete")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(BODY_STATUS))
                .andExpect(jsonPath("$.data.message").value("회원 탈퇴가 완료되었습니다."));

        User deletedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(deletedUser.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_TOKEN.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.name()));
    }

    @Test
    void 탈퇴회원_로그인_실패한다() throws Exception {
        // given
        String email = uniqueEmail();
        signup(email, "Password123");
        String accessToken = loginAccessToken(email, "Password123");

        mockMvc.perform(post("/api/users/delete")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123"
                                }
                                """.formatted(email)))
                .andExpect(status().is(ErrorCode.INVALID_LOGIN_CREDENTIALS.getHttpStatus().value()))
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_LOGIN_CREDENTIALS.getHttpStatus().value()))
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_LOGIN_CREDENTIALS.name()));
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

    private void assertResponseKeyOrder(MvcResult result, String... keys) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        int previousIndex = -1;

        for (String key : keys) {
            int currentIndex = content.indexOf(key);

            assertThat(currentIndex)
                    .as("%s 키가 응답에 있어야 합니다. response=%s", key, content)
                    .isNotNegative();
            assertThat(currentIndex)
                    .as("%s 키가 이전 키보다 뒤에 있어야 합니다. response=%s", key, content)
                    .isGreaterThan(previousIndex);

            previousIndex = currentIndex;
        }
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }
}
