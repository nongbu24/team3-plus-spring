package com.example.team3plusspring.domain.cart.controller;

import com.example.team3plusspring.domain.cart.dto.AddCartItemRequest;
import com.example.team3plusspring.domain.cart.dto.AddCartItemResponse;
import com.example.team3plusspring.domain.cart.service.CartService;
import com.example.team3plusspring.global.response.ApiResponse;
import com.example.team3plusspring.global.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<AddCartItemResponse>> addCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid AddCartItemRequest request
    ) {
        AddCartItemResponse response = cartService.add(userDetails, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, response));
    }
}
