package com.example.team3plusspring.domain.auth.facade;

import com.example.team3plusspring.domain.auth.dto.LoginRequest;
import com.example.team3plusspring.domain.auth.dto.LoginResponse;
import com.example.team3plusspring.domain.auth.dto.LogoutResponse;
import com.example.team3plusspring.domain.auth.dto.SignupRequest;
import com.example.team3plusspring.domain.auth.dto.SignupResponse;
import com.example.team3plusspring.domain.auth.service.AuthService;
import com.example.team3plusspring.domain.cart.service.CartService;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.service.UserService;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuthFacade {
    private final AuthService authService;
    private final UserService userService;
    private final CartService cartService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User savedUser = userService.createUser(
                request.getEmail(),
                authService.encodePassword(request.getPassword()),
                request.getName(),
                request.getPhone()
        );
        cartService.createCart(savedUser);

        return SignupResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userService.findActiveUserByEmail(request.getEmail())
                .orElseThrow(authService::invalidLoginCredentials);

        authService.validatePassword(request.getPassword(), user.getPassword());

        return authService.createLoginResponse(user.getId());
    }

    public LogoutResponse logout() {
        return authService.logout();
    }
}
