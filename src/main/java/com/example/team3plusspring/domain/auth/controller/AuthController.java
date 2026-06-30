package com.example.team3plusspring.domain.auth.controller;

import com.example.team3plusspring.domain.auth.dto.LoginRequest;
import com.example.team3plusspring.domain.auth.dto.LoginResponse;
import com.example.team3plusspring.domain.auth.dto.LogoutResponse;
import com.example.team3plusspring.domain.auth.dto.SignupRequest;
import com.example.team3plusspring.domain.auth.dto.SignupResponse;
import com.example.team3plusspring.domain.auth.facade.AuthFacade;
import com.example.team3plusspring.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthFacade authFacade;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authFacade.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authFacade.login(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout() {
        LogoutResponse response = authFacade.logout();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
