package com.example.team3plusspring.domain.point.repository;

import com.example.team3plusspring.domain.point.entity.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    Optional<PointAccount> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
