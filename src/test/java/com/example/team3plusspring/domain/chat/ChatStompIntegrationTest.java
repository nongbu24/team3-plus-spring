package com.example.team3plusspring.domain.chat;

import com.example.team3plusspring.domain.chat.entity.ChatMember;
import com.example.team3plusspring.domain.chat.entity.ChatRoom;
import com.example.team3plusspring.domain.chat.repository.ChatMemberRepository;
import com.example.team3plusspring.domain.chat.repository.ChatRoomRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.security.jwt.JwtTokenProvider;
import com.example.team3plusspring.support.RedisTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import com.jayway.jsonpath.JsonPath;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatStompIntegrationTest extends RedisTestSupport {
    private static final String PASSWORD = "Password123";
    private static final int TIMEOUT_SECONDS = 3;

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChatRoomRepository chatRoomRepository;

    @Autowired
    ChatMemberRepository chatMemberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    final List<StompSession> sessions = new ArrayList<>();

    @AfterEach
    void tearDown() {
        sessions.stream()
                .filter(StompSession::isConnected)
                .forEach(StompSession::disconnect);
        sessions.clear();
    }

    @Test
    void 웹소켓연결_토큰이없으면_실패한다() throws Exception {
        // given
        StompConnectionResult result = connect(null);

        // when
        boolean failed = result.awaitFailure();

        // then
        assertThat(failed).isTrue();
        assertThat(result.isConnected()).isFalse();
    }

    @Test
    void 채팅방구독_접근권한이없으면_실패한다() throws Exception {
        // given
        User owner = saveUser("방주인");
        User otherUser = saveUser("다른회원");
        ChatRoom room = saveRoom(owner);
        StompSession otherSession = connect(jwtTokenProvider.createAccessToken(otherUser.getId())).getConnectedSession();
        StompSession ownerSession = connect(jwtTokenProvider.createAccessToken(owner.getId())).getConnectedSession();
        CompletableFuture<String> receivedMessage = new CompletableFuture<>();

        otherSession.subscribe("/sub/chat/" + room.getId(), new ChatMessageFrameHandler(receivedMessage));
        waitBrieflyForSubscription();

        // when
        sendChatMessage(ownerSession, room.getId(), "권한 없는 사용자는 받으면 안 되는 메시지");

        // then
        assertThatThrownBy(() -> receivedMessage.get(1, TimeUnit.SECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);
    }

    @Test
    void 채팅메시지전송_정상사용자이면_구독자가메시지를수신한다() throws Exception {
        // given
        User user = saveUser("홍길동");
        ChatRoom room = saveRoom(user);
        StompSession userSession = connect(jwtTokenProvider.createAccessToken(user.getId())).getConnectedSession();

        CompletableFuture<String> receivedMessage = new CompletableFuture<>();
        userSession.subscribe("/sub/chat/" + room.getId(), new ChatMessageFrameHandler(receivedMessage));
        waitBrieflyForSubscription();

        // when
        sendChatMessage(userSession, room.getId(), "안녕하세요");

        // then
        String response = receivedMessage.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(JsonPath.<String>read(response, "$.content")).isEqualTo("안녕하세요");
        assertThat(JsonPath.<Integer>read(response, "$.senderId").longValue()).isEqualTo(user.getId());
        assertThat(JsonPath.<String>read(response, "$.senderName")).isEqualTo("홍길동");
        assertThat(JsonPath.<Number>read(response, "$.messageId")).isNotNull();
    }

    private void sendChatMessage(StompSession session, Long roomId, String content) {
        StompHeaders sendHeaders = new StompHeaders();
        sendHeaders.setDestination("/pub/chat.send");
        sendHeaders.setContentType(MimeTypeUtils.APPLICATION_JSON);
        String payload = """
                {
                  "roomId": %d,
                  "content": "%s"
                }
                """.formatted(roomId, content);
        session.send(sendHeaders, payload.getBytes(StandardCharsets.UTF_8));
    }

    private StompConnectionResult connect(String accessToken) {
        WebSocketClient webSocketClient = new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())));
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new JsonByteArrayMessageConverter());

        StompHeaders headers = new StompHeaders();
        if (accessToken != null) {
            headers.add("Authorization", "Bearer " + accessToken);
        }

        StompConnectionResult result = new StompConnectionResult();
        stompClient.connectAsync(webSocketUrl(), new WebSocketHttpHeaders(), headers, result)
                .whenComplete((connectedSession, throwable) -> {
                    if (throwable != null) {
                        result.fail(throwable);
                        return;
                    }

                    result.connected(connectedSession);
                });

        return result;
    }

    private String webSocketUrl() {
        return "http://localhost:" + port + "/ws";
    }

    private void waitBrieflyForSubscription() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(300);
    }

    private User saveUser(String name) {
        return userRepository.save(User.create(
                uniqueEmail(),
                passwordEncoder.encode(PASSWORD),
                name,
                "010-1234-5678"
        ));
    }

    private ChatRoom saveRoom(User owner) {
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(owner));
        chatMemberRepository.save(ChatMember.join(room, owner));

        return room;
    }

    private String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private static class ChatMessageFrameHandler implements StompFrameHandler {
        private final CompletableFuture<String> receivedMessage;

        private ChatMessageFrameHandler(CompletableFuture<String> receivedMessage) {
            this.receivedMessage = receivedMessage;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            receivedMessage.complete(new String((byte[]) payload, StandardCharsets.UTF_8));
        }
    }

    private static class JsonByteArrayMessageConverter extends ByteArrayMessageConverter {
        private JsonByteArrayMessageConverter() {
            addSupportedMimeTypes(MimeTypeUtils.APPLICATION_JSON);
        }
    }

    private class StompConnectionResult extends StompSessionHandlerAdapter {
        private final CountDownLatch connectedLatch = new CountDownLatch(1);
        private final CountDownLatch failureLatch = new CountDownLatch(1);
        private volatile StompSession connectedSession;

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            connected(session);
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            fail(new IllegalStateException(String.valueOf(payload)));
        }

        @Override
        public void handleException(
                StompSession session,
                org.springframework.messaging.simp.stomp.StompCommand command,
                StompHeaders headers,
                byte[] payload,
                Throwable exception
        ) {
            fail(exception);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            fail(exception);
        }

        private void connected(StompSession session) {
            this.connectedSession = session;
            connectedLatch.countDown();
        }

        private void fail(Throwable throwable) {
            failureLatch.countDown();
        }

        private StompSession getConnectedSession() throws Exception {
            assertThat(connectedLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
            assertThat(connectedSession).isNotNull();

            sessions.add(connectedSession);
            return connectedSession;
        }

        private boolean awaitFailure() throws InterruptedException {
            return failureLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        private boolean isConnected() {
            return connectedSession != null && connectedSession.isConnected();
        }
    }
}
