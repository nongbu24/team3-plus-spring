package com.example.team3plusspring.domain.product.entity;

import com.example.team3plusspring.global.entity.BaseEntity;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @Column(name = "category_id")
    private Long categoryId;

    private Product(String name, String description, int price, int stock, ProductStatus status, Long categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.categoryId = categoryId;
    }

    public static Product create(String name, String description, int price, int stock, Long categoryId) {
        return new Product(name, description, price, stock, ProductStatus.ON_SALE, categoryId);
    }

    // 재고 차감 메서드
    public void decreaseStock(int quantity) {
        validateStatus();

        if (!hasEnoughStock(quantity) || quantity <= 0) {
            throw new BusinessException(ErrorCode.ORDER_STOCK_SHORTAGE);
        }

        this.stock -= quantity;
    }

    public boolean hasEnoughStock(int quantity) {
        return this.stock >= quantity;
    }

    public void validateStatus() {
        if (this.status != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
    }
}