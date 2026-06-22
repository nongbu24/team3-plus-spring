package com.example.team3plusspring.domain.auth.dto;

import java.time.LocalDateTime;

public record SignupResponse(
        Long userId,
        String email,
        String name,
        String phone,
        LocalDateTime createdAt
) {
}
