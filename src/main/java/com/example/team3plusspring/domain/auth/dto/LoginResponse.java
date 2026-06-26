package com.example.team3plusspring.domain.auth.dto;

import com.example.team3plusspring.domain.user.entity.User;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final String tokenType;
    private final String accessToken;
    private final long expiresIn;
    private final UserSummary user;

    private LoginResponse(String tokenType, String accessToken, long expiresIn, UserSummary user) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public static LoginResponse of(String accessToken, long expiresIn, User user) {
        return new LoginResponse("Bearer", accessToken, expiresIn, UserSummary.from(user));
    }

    @Getter
    public static class UserSummary {

        private final Long userId;
        private final String email;
        private final String name;

        private UserSummary(Long userId, String email, String name) {
            this.userId = userId;
            this.email = email;
            this.name = name;
        }

        private static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getEmail(), user.getName());
        }
    }
}
