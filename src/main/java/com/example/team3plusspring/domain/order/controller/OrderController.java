package com.example.team3plusspring.domain.order.controller;

import com.example.team3plusspring.domain.order.dto.CreateDirectOrderRequest;
import com.example.team3plusspring.domain.order.dto.CreateOrderFromCartRequest;
import com.example.team3plusspring.domain.order.dto.CreateOrderResponse;
import com.example.team3plusspring.domain.order.facade.OrderFacade;
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
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createDirectOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody  @Valid CreateDirectOrderRequest request
    ) {
        CreateOrderResponse response = orderFacade.createDirectOrder(userDetails.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, response));

    }

    @PostMapping("/carts")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrderFromCart(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CreateOrderFromCartRequest request
    ) {
        CreateOrderResponse response = orderFacade.createOrderFromCart(userDetails.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, response));
    }

}
