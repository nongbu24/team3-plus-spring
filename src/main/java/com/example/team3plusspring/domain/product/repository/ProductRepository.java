package com.example.team3plusspring.domain.product.repository;

import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p WHERE " +
            "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
            "(:keyword IS NULL OR p.name LIKE %:keyword%) AND " +
            "(:status IS NULL OR p.status = :status)")
    Page<Product> findByCondition(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);
}