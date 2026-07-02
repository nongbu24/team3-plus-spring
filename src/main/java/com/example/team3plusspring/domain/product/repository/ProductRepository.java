package com.example.team3plusspring.domain.product.repository;

import com.example.team3plusspring.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductQueryRepository {

    @Query("SELECT p FROM Product p WHERE " +
            "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%) AND " +
            "(:status IS NULL OR p.status = :status)")
    Page<Product> findByChatbotKeyword(
            @Param("keyword") String keyword,
            @Param("status") ProductStatus status,
            Pageable pageable
    );

    Page<Product> findByCategoryIdInAndStatus(List<Long> categoryIds, ProductStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :productIds order by p.id")
    List<Product> findAllByIdInForUpdate(@Param("productIds") List<Long> productIds);
}