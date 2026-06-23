package com.example.team3plusspring.domain.product.controller;

import com.example.team3plusspring.domain.product.dto.GetOneProductResponse;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public ApiResponse<GetOneProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }
}