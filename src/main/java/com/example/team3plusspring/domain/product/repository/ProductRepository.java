package com.example.team3plusspring.domain.product.repository;

import com.example.team3plusspring.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductQueryRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :productIds order by p.id")
    List<Product> findAllByIdInForUpdate(@Param("productIds") List<Long> productIds);
}

    // 인기 상품 조회
// 조회수가 높은 순서대로 가져오기 (판매중/품절 상태만 노출, 단종 상품 제외)
    @Query("SELECT p FROM Product p WHERE p.status IN :statuses ORDER BY p.viewCount DESC")
    List<Product> findPopularProducts(@Param("statuses") List<ProductStatus> statuses, Pageable pageable);

    // 조회수 증가를 위한 직접 업데이트 쿼리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
