package com.example.team3plusspring.domain.chat.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotServiceTest {

    private final ChatbotService chatbotService = new ChatbotService(null, null);

    @Test
    void 키워드후보생성_질문에서_핵심단어와조합과전체질문을_순서대로반환한다() {
        // when
        List<String> keywords = chatbotService.createKeywordCandidates("운동할 때 입기 좋은 티셔츠 추천해주세요!");

        // then
        assertThat(keywords).containsExactly(
                "티셔츠",
                "운동 입기 티셔츠",
                "운동",
                "입기",
                "운동할 때 입기 좋은 티셔츠 추천해주세요"
        );
    }

    @Test
    void 키워드후보생성_특수문자를_제거한다() {
        // when
        List<String> keywords = chatbotService.createKeywordCandidates("노트북@@ 추천해줘??");

        // then
        assertThat(keywords).containsExactly(
                "노트북",
                "노트북 추천해줘"
        );
    }

    @Test
    void 키워드후보생성_비교요청문장에서_카테고리명을_우선검색어로반환한다() {
        // when
        List<String> keywords = chatbotService.createKeywordCandidates("판매하고 있는 노트북들 설명 요약해서 비교해줘");

        // then
        assertThat(keywords).containsExactly(
                "노트북",
                "판매하고 있는 노트북들 설명 요약해서 비교해줘"
        );
    }

    @Test
    void 키워드후보생성_한글로쓴_영문상품명을_검색후보에_포함한다() {
        // when
        List<String> keywords = chatbotService.createKeywordCandidates("갤럭시 s25랑 아이폰 16 스펙 비교해줘");

        // then
        assertThat(keywords).containsSubsequence("Galaxy S25", "iPhone 16");
    }
}
