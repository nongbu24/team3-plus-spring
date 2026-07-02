package com.example.team3plusspring.domain.chat.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatbotTopic {
    PRODUCT_RECOMMENDATION(
            "상품 추천",
            "사용자 상황에 맞는 상품 선택 기준과 추천 방향을 답변하세요. 정보가 부족해도 추가 질문으로 끝내지 말고, 용도별 선택 기준을 나누어 안내하세요."
    ),
    PRODUCT_SUMMARY(
            "상품 설명 요약",
            "상품명이나 상품 정보가 있으면 핵심 특징, 적합한 사용자, 확인할 점을 요약하세요. 상품 정보가 부족하면 어떤 항목을 보면 되는지 안내하세요."
    ),
    PRE_CART_QUESTION(
            "장바구니 담기 전 질문 답변",
            "구매 전에 확인해야 할 호환성, 옵션, 수량, 배송, 교환, 환불 같은 실무적인 체크포인트를 답변하세요."
    ),
    PRODUCT_COMPARISON(
            "특정 상품 비교",
            "비교 대상이 있으면 차이점, 선택 기준, 어떤 사용자에게 더 맞는지 답변하세요. 비교 대상이 부족하면 대표 기준별 비교 방법을 안내하세요."
    );

    private final String label;
    private final String instruction;
}
