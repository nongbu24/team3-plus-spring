package com.example.team3plusspring.domain.product.service;

import com.example.team3plusspring.domain.product.dto.ProductResponse;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 상품 상세 조회
    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        return ProductResponse.from(product);
    }
}