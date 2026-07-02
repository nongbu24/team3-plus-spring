package com.example.team3plusspring.domain.product.repository;

import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductQueryRepository {
    Page<Product> findByCondition(Long categoryId, String keyword, ProductStatus status, Pageable pageable);
    List<Product> findPopularProducts(List<ProductStatus> statuses, Pageable pageable);
    void incrementViewCount(Long id);
}