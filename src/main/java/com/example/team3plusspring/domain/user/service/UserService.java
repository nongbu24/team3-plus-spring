package com.example.team3plusspring.domain.user.service;

import com.example.team3plusspring.domain.user.dto.UserDeleteResponse;
import com.example.team3plusspring.domain.user.dto.UserMeResponse;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMyInfo(Long userId) {
        User user = findActiveUser(userId);

        return UserMeResponse.from(user);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User createUser(String email, String password, String name, String phone) {
        User user = User.create(email, password, name, phone);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findActiveUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email);
    }

    @Transactional(readOnly = true)
    public void validateActiveUser(Long userId) {
        findActiveUser(userId);
    }

    @Transactional
    public UserDeleteResponse deleteMe(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isDeleted()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
        }

        user.delete();

        return UserDeleteResponse.success();
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
