package com.example.team3plusspring.domain.product.dto;

import com.example.team3plusspring.domain.category.entity.Category;
import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GetOneProductResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final int price;
    private final int stock;
    private final ProductStatus status;
    private final Long categoryId;
    private final String categoryName;

    public static GetOneProductResponse of(Product product, Category category) {
        return new GetOneProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                category.getId(),
                category.getName()
        );
    }
}