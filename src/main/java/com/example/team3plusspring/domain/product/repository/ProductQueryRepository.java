package com.example.team3plusspring.domain.product.repository;

import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductQueryRepository {
    Page<Product> findByCondition(Long categoryId, String keyword, ProductStatus status, Pageable pageable);

    Page<Product> findByChatbotKeyword(String keyword, ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdInAndStatus(List<Long> categoryIds, ProductStatus status, Pageable pageable);

    List<Product> findPopularProducts(List<ProductStatus> statuses, Pageable pageable);

    Optional<Product> findByIdForUpdate(Long productId);

    List<Product> findAllByIdInForUpdate(List<Long> productIds);

    void incrementViewCount(Long id);
}
