package com.example.team3plusspring.domain.point.service;

import com.example.team3plusspring.domain.point.entity.PointAccount;
import com.example.team3plusspring.domain.point.repository.PointAccountRepository;
import com.example.team3plusspring.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointAccountService {
    private final PointAccountRepository pointAccountRepository;

    @Transactional
    public PointAccount createPointAccount(User user) {
        return pointAccountRepository.save(PointAccount.create(user));
    }
}
