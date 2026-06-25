package com.example.team3plusspring.domain.auth.service;

import com.example.team3plusspring.domain.auth.dto.*;
import com.example.team3plusspring.domain.cart.entity.Cart;
import com.example.team3plusspring.domain.cart.repository.CartRepository;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import com.example.team3plusspring.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName(),
                request.getPhone()
        );
        User savedUser = userRepository.save(user);
        cartRepository.save(Cart.create(savedUser));

        return SignupResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(this::invalidLoginCredentials);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw invalidLoginCredentials();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        return new LoginResponse(
                "Bearer",
                accessToken,
                jwtTokenProvider.getAccessTokenExpiresInSeconds(),
                new LoginResponse.UserSummary(
                        user.getId(),
                        user.getEmail(),
                        user.getName()
                )
        );
    }

    public LogoutResponse logout() {
        return LogoutResponse.success();
    }

    private BusinessException invalidLoginCredentials() {
        return new BusinessException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
    }
}
