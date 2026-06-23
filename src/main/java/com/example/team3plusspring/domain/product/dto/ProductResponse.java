package com.example.team3plusspring.domain.product.dto;

import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import lombok.Getter;

@Getter
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private ProductStatus status;
    private String category;

    private ProductResponse() {}

    public static ProductResponse from(Product product) {
        ProductResponse response = new ProductResponse();
        response.id = product.getId();
        response.name = product.getName();
        response.description = product.getDescription();
        response.price = product.getPrice();
        response.stock = product.getStock();
        response.status = product.getStatus();
        response.category = product.getCategory();
        return response;
    }
}