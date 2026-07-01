package com.example.team3plusspring.domain.chat.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.example.team3plusspring.domain.chat.entity.ChatbotTopic;
import com.example.team3plusspring.domain.product.dto.ChatbotProductResponse;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_HISTORY_SESSIONS = 10_000;
    private static final Duration HISTORY_EXPIRE_AFTER_ACCESS = Duration.ofMinutes(30);
    private static final int MAX_RELATED_PRODUCTS = 3;
    private static final int MAX_KEYWORD_CANDIDATES = 5;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final Set<String> KEYWORD_STOP_WORDS = Set.of(
            "추천", "추천해줘", "추천해주세요", "알려줘", "알려주세요", "있나요", "있어", "상품",
            "제품", "가격", "얼마", "비교", "비교해줘", "비교해주세요", "설명", "요약", "요약해서",
            "판매", "판매하는", "판매하고", "있는", "좋은", "괜찮은", "찾아줘", "찾아주세요", "스펙"
    );
    private static final Map<String, List<String>> PRODUCT_KEYWORD_ALIASES = Map.ofEntries(
            Map.entry("갤럭시", List.of("Galaxy")),
            Map.entry("galaxy", List.of("갤럭시")),
            Map.entry("아이폰", List.of("iPhone")),
            Map.entry("iphone", List.of("아이폰")),
            Map.entry("맥북", List.of("MacBook")),
            Map.entry("macbook", List.of("맥북")),
            Map.entry("에어팟", List.of("AirPods")),
            Map.entry("airpods", List.of("에어팟")),
            Map.entry("핸드폰", List.of("스마트폰")),
            Map.entry("휴대폰", List.of("스마트폰")),
            Map.entry("폰", List.of("스마트폰")),
            Map.entry("패드", List.of("태블릿")),
            Map.entry("탭", List.of("태블릿")),
            Map.entry("컴퓨터", List.of("데스크탑", "노트북")),
            Map.entry("pc", List.of("데스크탑", "PC 부품")),
            Map.entry("모니터링", List.of("모니터")),
            Map.entry("키보드", List.of("키보드/마우스")),
            Map.entry("마우스", List.of("키보드/마우스")),
            Map.entry("이어셋", List.of("이어폰/헤드폰")),
            Map.entry("헤드셋", List.of("이어폰/헤드폰")),
            Map.entry("무선이어폰", List.of("이어폰/헤드폰")),
            Map.entry("워치", List.of("스마트워치")),
            Map.entry("시계", List.of("스마트워치")),
            Map.entry("게임기", List.of("게임기/콘솔")),
            Map.entry("콘솔", List.of("게임기/콘솔")),
            Map.entry("게임패드", List.of("게임기/콘솔")),
            Map.entry("외장하드", List.of("저장장치")),
            Map.entry("ssd", List.of("저장장치")),
            Map.entry("usb", List.of("저장장치")),
            Map.entry("공유기", List.of("네트워크 장비")),
            Map.entry("와이파이", List.of("네트워크 장비")),
            Map.entry("케이블", List.of("충전기/케이블")),
            Map.entry("충전", List.of("충전기/케이블")),
            Map.entry("가전", List.of("생활가전")),
            Map.entry("주방", List.of("주방가전")),
            Map.entry("선풍기", List.of("계절가전")),
            Map.entry("케이스", List.of("액세서리"))
    );

    private final ChatClient chatClient;
    private final ProductService productService;

    // 비로그인 챗봇은 sessionId가 계속 새로 생길 수 있으므로, TTL과 최대 개수로 메모리 증가를 제한한다.
    private final Cache<String, List<Message>> historyCache = Caffeine.newBuilder()
            .expireAfterAccess(HISTORY_EXPIRE_AFTER_ACCESS)
            .maximumSize(MAX_HISTORY_SESSIONS)
            .build();

    public String chat(String sessionId, ChatbotTopic topic, String userMessage) {

        // 세션 히스토리 가져오기 (없으면 새로 생성)
        List<Message> history = historyCache.get(
                sessionId,
                key -> Collections.synchronizedList(new ArrayList<>())
        );

        String prompt = createPrompt(topic, userMessage);

        List<Message> historySnapshot;
        synchronized (history) {
            historySnapshot = new ArrayList<>(history);
        }

        String response;

        try {
            response = chatClient.prompt()
                    .messages(historySnapshot)
                    .user(prompt)
                    .call()
                    .content();
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        if (response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILED);
        }

        String normalizedResponse = normalizeResponse(response);

        synchronized (history) {
            history.add(new UserMessage(userMessage));
            history.add(new AssistantMessage(normalizedResponse));
            trimHistory(history);
        }

        return normalizedResponse;
    }

    private void trimHistory(List<Message> history) {
        int overflow = history.size() - MAX_HISTORY_MESSAGES;

        if (overflow > 0) {
            history.subList(0, overflow).clear();
        }
    }

    private String createPrompt(ChatbotTopic topic, String userMessage) {
        List<ChatbotProductResponse> relatedProducts = findRelatedProducts(userMessage);
        String topicGuide = "상담 유형: " + topic.getLabel() + "\n" +
                "상담 유형별 답변 규칙: " + topic.getInstruction() + "\n" +
                "선택된 상담 유형과 관련 없는 내용은 답변하지 말고, 선택한 버튼에 맞는 질문만 도와드릴 수 있다고 안내하세요.\n" +
                "사용자가 고마움이나 긍정적인 반응을 표현하면 '감사합니다.'로 정중하게 받은 뒤 바로 답변을 이어가세요.\n" +
                "일반 상품 질문에는 '안녕하세요.', '감사합니다.' 같은 인사말이나 감사 표현으로 시작하지 말고 바로 핵심 답변을 시작하세요.\n" +
                "답변할 때 사용자를 다시 질문으로 몰아가지 말고, 현재 정보로 줄 수 있는 완성된 답변을 먼저 제공하세요.\n" +
                "장바구니에 담기, 구매하기, 쇼핑몰에서 확인하기처럼 직접 구매를 유도하는 문장은 사용하지 마세요.\n" +
                "마지막 문장은 질문이나 구매 유도가 아니라 중립적인 요약 또는 선택 기준으로 마무리하세요.\n\n";

        if (relatedProducts.isEmpty()) {
            return topicGuide + "사용자 질문: " + userMessage + "\n\n" +
                    "현재 상품 DB에서 사용자 질문과 직접 관련된 상품을 찾지 못했습니다.\n" +
                    "실제 상품명, 가격, 재고, 카테고리를 절대 만들어내지 마세요.\n" +
                    "상품 추천은 하지 말고, 일반적인 선택 기준만 안내하세요.";
        }

        return topicGuide + "사용자 질문: " + userMessage + "\n\n" +
                "아래는 현재 상품 DB에서 검색한 관련 상품입니다.\n" +
                "반드시 아래 상품 목록에 포함된 상품만 추천하세요.\n" +
                "목록에 없는 상품명, 가격, 재고, 카테고리, 할인 정보, 배송 정보는 절대 만들어내지 마세요.\n" +
                "상품 목록에 없는 정보는 '제공된 상품 정보만으로는 확인할 수 없습니다'라고 답하세요.\n" +
                "사용자 질문과 관련된 상품이 목록에 부족하면, 억지로 추천하지 말고 제공된 상품 기준에서만 비교하세요.\n\n" +
                formatRelatedProducts(relatedProducts);
    }

    private List<ChatbotProductResponse> findRelatedProducts(String userMessage) {
        Map<String, ChatbotProductResponse> relatedProducts = new LinkedHashMap<>();

        for (String keyword : createKeywordCandidates(userMessage)) {
            productService.searchProductsForChatbot(keyword, MAX_RELATED_PRODUCTS)
                    .forEach(product -> relatedProducts.putIfAbsent(product.getName(), product));

            if (relatedProducts.size() >= MAX_RELATED_PRODUCTS) {
                break;
            }
        }

        return relatedProducts.values()
                .stream()
                .limit(MAX_RELATED_PRODUCTS)
                .toList();
    }

    List<String> createKeywordCandidates(String userMessage) {
        String normalized = normalizeKeyword(userMessage);

        if (normalized.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        List<String> meaningfulTokens = normalized.lines()
                .flatMap(line -> List.of(line.split("\\s+")).stream())
                .map(this::removeCommonSuffix)
                .filter(token -> token.length() >= 2)
                .filter(token -> !KEYWORD_STOP_WORDS.contains(token))
                .toList();

        if (!meaningfulTokens.isEmpty()) {
            addProductAliasCandidates(candidates, meaningfulTokens);
            candidates.add(meaningfulTokens.get(meaningfulTokens.size() - 1));
        }

        if (meaningfulTokens.size() >= 2) {
            candidates.add(limitKeywordLength(String.join(" ", meaningfulTokens)));
        }

        if (meaningfulTokens.size() >= 2) {
            candidates.addAll(meaningfulTokens.subList(0, meaningfulTokens.size() - 1));
        }

        candidates.add(limitKeywordLength(normalized));

        return candidates.stream()
                .limit(MAX_KEYWORD_CANDIDATES)
                .toList();
    }

    private String normalizeKeyword(String userMessage) {
        return userMessage
                .replaceAll("[^0-9A-Za-z가-힣\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String removeCommonSuffix(String token) {
        return token
                .replaceAll("(하기|할)$", "")
                .replaceAll("(으로|로|에서|에게|에는|은|는|이|가|을|를|과|와|랑|도|만)$", "")
                .replaceAll("들$", "");
    }

    private void addProductAliasCandidates(LinkedHashSet<String> candidates, List<String> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            List<String> aliases = findAliases(tokens.get(i));

            if (aliases.isEmpty()) {
                continue;
            }

            if (i + 1 < tokens.size()) {
                String modelToken = normalizeModelToken(tokens.get(i + 1));
                candidates.add(limitKeywordLength(tokens.get(i) + " " + modelToken));
                aliases.forEach(alias -> candidates.add(limitKeywordLength(alias + " " + modelToken)));
            }
        }

        for (String token : tokens) {
            List<String> aliases = findAliases(token);

            if (aliases.isEmpty()) {
                continue;
            }

            candidates.add(token);
            candidates.addAll(aliases);
        }
    }

    private List<String> findAliases(String token) {
        return PRODUCT_KEYWORD_ALIASES.getOrDefault(token.toLowerCase(), List.of());
    }

    private String normalizeModelToken(String token) {
        if (token.matches("(?i)[a-z]+\\d+[a-z0-9]*")) {
            return token.toUpperCase();
        }

        return token;
    }

    private String limitKeywordLength(String keyword) {
        if (keyword.length() <= MAX_KEYWORD_LENGTH) {
            return keyword;
        }

        return keyword.substring(0, MAX_KEYWORD_LENGTH);
    }

    private String formatRelatedProducts(List<ChatbotProductResponse> products) {
        return products.stream()
                .map(product -> "- 상품명: " + product.getName() +
                        ", 설명: " + product.getDescription() +
                        ", 가격: " + product.getPrice() + "원" +
                        ", 재고: " + product.getStock() + "개" +
                        ", 카테고리: " + product.getCategoryName())
                .collect(Collectors.joining("\n"));
    }

    private String normalizeResponse(String response) {
        String plainText = response.lines()
                .map(String::trim)
                .map(line -> line.replaceAll("^#{1,6}\\s*", ""))
                .map(line -> line.replaceAll("^[-*+]\\s+", ""))
                .map(line -> line.replaceAll("^\\d+[.)]\\s+", ""))
                .map(line -> line.replace("**", ""))
                .map(line -> line.replace("__", ""))
                .map(line -> line.replace("`", ""))
                .map(line -> line.replaceAll("[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}]", ""))
                .collect(Collectors.joining(" "));

        return plainText
                .replaceAll("\\s+", " ")
                .replaceAll("([.!?。！？])\\s*", "$1\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
