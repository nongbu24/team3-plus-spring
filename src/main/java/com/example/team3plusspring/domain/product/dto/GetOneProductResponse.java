package com.example.team3plusspring.domain.product.dto;

import com.example.team3plusspring.domain.product.entity.Product;
import com.example.team3plusspring.domain.product.entity.ProductStatus;
import lombok.Getter;

@Getter
public class GetOneProductResponse {

    private Long id;
    private String name;
    private String description;
    private Integer price;
    private Integer stock;
    private ProductStatus status;
    private String category;

    private GetOneProductResponse() {}

    public static GetOneProductResponse from(Product product) {
        GetOneProductResponse response = new GetOneProductResponse();
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