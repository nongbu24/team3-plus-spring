package com.example.team3plusspring.domain.auth.service;

import com.example.team3plusspring.domain.auth.dto.LoginResponse;
import com.example.team3plusspring.domain.auth.dto.LogoutResponse;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw invalidLoginCredentials();
        }
    }

    public LoginResponse createLoginResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        return LoginResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds(),
                user
        );
    }

    public LogoutResponse logout() {
        return LogoutResponse.success();
    }

    public BusinessException invalidLoginCredentials() {
        return new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }
}
