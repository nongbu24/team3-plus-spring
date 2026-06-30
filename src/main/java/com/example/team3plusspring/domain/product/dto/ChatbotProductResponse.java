package com.example.team3plusspring.domain.product.dto;

import com.example.team3plusspring.domain.category.entity.Category;
import com.example.team3plusspring.domain.product.entity.Product;
import lombok.Getter;

@Getter
public class ChatbotProductResponse {
    private final String name;
    private final String description;
    private final int price;
    private final int stock;
    private final String categoryName;

    private ChatbotProductResponse(String name, String description, int price, int stock, String categoryName) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.categoryName = categoryName;
    }

    public static ChatbotProductResponse of(Product product, Category category) {
        return new ChatbotProductResponse(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                category.getName()
        );
    }
}
