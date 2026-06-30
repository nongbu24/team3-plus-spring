package com.example.team3plusspring.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@Getter
@JsonPropertyOrder({"tokenType", "accessToken"})
public class LoginResponse {
    private final String tokenType;
    private final String accessToken;

    private LoginResponse(String tokenType, String accessToken) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
    }

    public static LoginResponse of(String accessToken) {
        return new LoginResponse("Bearer", accessToken);
    }
}
