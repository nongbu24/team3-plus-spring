package com.example.team3plusspring.domain.portone.dto;

import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PortOneCancelRequest {

    private final String reason;  // [필수] 취소 사유
    private final String storeId; // [조건부] 하위 상점 사용 시 필수

    // PortOne 결제 취소 요청 정보를 생성한다.
    public static PortOneCancelRequest of(String reason, String storeId) {
        return new PortOneCancelRequest(reason, storeId);
    }
}
