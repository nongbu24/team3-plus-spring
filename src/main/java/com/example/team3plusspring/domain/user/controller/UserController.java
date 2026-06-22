package com.example.team3plusspring.domain.user.controller;

import com.example.team3plusspring.domain.user.dto.UserDeleteResponse;
import com.example.team3plusspring.domain.user.dto.UserMeResponse;
import com.example.team3plusspring.domain.user.service.UserService;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserMeResponse response = userService.getMyInfo(userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<UserDeleteResponse>> deleteMe(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserDeleteResponse response = userService.deleteMe(userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
