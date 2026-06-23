package com.example.team3plusspring.domain.product.repository;

import com.example.team3plusspring.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}