package com.example.team3plusspring.domain.category.repository;

import com.example.team3plusspring.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByNameContaining(String keyword);
}
