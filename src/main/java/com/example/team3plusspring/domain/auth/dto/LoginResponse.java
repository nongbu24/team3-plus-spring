package com.example.team3plusspring.domain.auth.dto;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String tokenType;
    private final String accessToken;
    private final long expiresIn;
    private final UserSummary user;

    public LoginResponse(String tokenType, String accessToken, long expiresIn, UserSummary user) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    @Getter
    public static class UserSummary {

        private final Long userId;
        private final String email;
        private final String name;

        public UserSummary(Long userId, String email, String name) {
            this.userId = userId;
            this.email = email;
            this.name = name;
        }
    }
}
