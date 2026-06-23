package com.example.team3plusspring.domain.product.controller;

import com.example.team3plusspring.domain.product.dto.ProductResponse;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.global.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // 상품 상세 조회
    @GetMapping("/products/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }
}