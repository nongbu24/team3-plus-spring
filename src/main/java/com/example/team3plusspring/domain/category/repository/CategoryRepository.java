package com.example.team3plusspring.domain.category.repository;

import com.example.team3plusspring.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
