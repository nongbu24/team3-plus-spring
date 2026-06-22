package com.example.team3plusspring.domain.user.repository;

import com.example.team3plusspring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
