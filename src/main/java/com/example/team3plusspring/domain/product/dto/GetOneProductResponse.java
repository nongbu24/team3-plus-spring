package com.example.team3plusspring.domain.product.dto;

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
    private final String category;

    public static GetOneProductResponse from(Product product) {
        return new GetOneProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCategory()
        );
    }
}