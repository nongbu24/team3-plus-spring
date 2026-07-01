package com.example.team3plusspring.domain.chat.service;

import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatbotRateLimiterTest {

    private final ChatbotRateLimiter chatbotRateLimiter = new ChatbotRateLimiter();

    @Test
    void 같은IP와_같은세션에서_분당10회를_초과하면_예외가_발생한다() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        for (int i = 0; i < 10; i++) {
            chatbotRateLimiter.checkAllowed("550e8400-e29b-41d4-a716-446655440000", request);
        }

        // when & then
        assertThatThrownBy(() -> chatbotRateLimiter.checkAllowed("550e8400-e29b-41d4-a716-446655440000", request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHATBOT_RATE_LIMIT_EXCEEDED)
                );
    }

    @Test
    void 서로다른IP는_각각_별도로_횟수를_계산한다() {
        // given
        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.setRemoteAddr("127.0.0.1");

        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.setRemoteAddr("127.0.0.2");

        for (int i = 0; i < 10; i++) {
            chatbotRateLimiter.checkAllowed("550e8400-e29b-41d4-a716-446655440000", firstRequest);
        }

        // when & then
        chatbotRateLimiter.checkAllowed("550e8400-e29b-41d4-a716-446655440000", secondRequest);
    }
}
