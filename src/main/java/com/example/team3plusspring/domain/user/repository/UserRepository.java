package com.example.team3plusspring.domain.user.repository;

import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findFirstByRoleAndDeletedAtIsNull(UserRole role);
}
