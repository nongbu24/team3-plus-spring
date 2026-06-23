package com.example.team3plusspring.domain.product.entity;

import com.example.team3plusspring.global.entity.BaseEntity;
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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    private String category;

    public static Product create(String name, String description, int price, int stock, String category) {
        Product product = new Product();
        product.name = name;
        product.description = description;
        product.price = price;
        product.stock = stock;
        product.status = ProductStatus.ON_SALE;
        product.category = category;
        return product;
    }
}