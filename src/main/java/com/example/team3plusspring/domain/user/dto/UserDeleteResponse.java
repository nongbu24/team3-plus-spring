package com.example.team3plusspring.domain.user.dto;

import lombok.Getter;

@Getter
public class UserDeleteResponse {

    private final String message;

    private UserDeleteResponse(String message) {
        this.message = message;
    }

    public static UserDeleteResponse success() {
        return new UserDeleteResponse("회원 탈퇴가 완료되었습니다.");
    }
}
