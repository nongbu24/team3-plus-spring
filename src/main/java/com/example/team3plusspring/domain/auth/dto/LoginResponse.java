package com.example.team3plusspring.domain.auth.dto;

public record LoginResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        UserSummary user
) {

    public record UserSummary(
            Long userId,
            String email,
            String name
    ) {
    }
}
