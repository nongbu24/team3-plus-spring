package com.example.team3plusspring.domain.product.dto;

import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class GetProductsResponse {

    private final Long id;
    private final String name;
    private final int price;
    private final int stock;
    private final ProductStatus status;
    private final Long categoryId;
    private final String categoryName;

    public static GetProductsResponse from(Product product) {
        return new GetProductsResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getCategory().getId(),
                product.getCategory().getName()
        );
    }
}
