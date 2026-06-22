package com.example.team3plusspring.domain.auth.dto;

import lombok.Getter;

@Getter
public class LogoutResponse {
    private final String message;

    private LogoutResponse(String message) {
        this.message = message;
    }

    public static LogoutResponse success() {
        return new LogoutResponse("로그아웃이 완료되었습니다.");
    }
}
