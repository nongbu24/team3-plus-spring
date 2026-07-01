package com.example.team3plusspring.domain.product.controller;

import com.example.team3plusspring.domain.product.dto.GetOneProductResponse;
import com.example.team3plusspring.domain.product.dto.GetProductsResponse;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import com.example.team3plusspring.domain.product.service.ProductService;
import com.example.team3plusspring.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class ProductController {

    private final ProductService productService;

    // 상품 상세 조회
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<GetOneProductResponse>> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProduct(productId)));
    }

    // 상품 목록 조회
    @GetMapping("/v1/products")
    public ResponseEntity<ApiResponse<Page<GetProductsResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "LATEST") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProducts(categoryId, keyword, status, sort, page, size)
        ));
    }

    // 인기 상품 조회 API (조회수 순)
    @GetMapping("/products/popular")
    public ResponseEntity<ApiResponse<List<GetProductsResponse>>> getPopularProducts(
            @RequestParam(defaultValue = "10")
            @Max(100) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getPopularProducts(limit)
        ));
    }
}