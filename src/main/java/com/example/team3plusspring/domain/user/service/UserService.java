package com.example.team3plusspring.domain.user.service;

import com.example.team3plusspring.domain.point.entity.PointAccount;
import com.example.team3plusspring.domain.point.repository.PointAccountRepository;
import com.example.team3plusspring.domain.user.dto.UserDeleteResponse;
import com.example.team3plusspring.domain.user.dto.UserMeResponse;
import com.example.team3plusspring.domain.user.entity.User;
import com.example.team3plusspring.domain.user.repository.UserRepository;
import com.example.team3plusspring.global.exception.BusinessException;
import com.example.team3plusspring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PointAccountRepository pointAccountRepository;

    @Transactional(readOnly = true)
    public UserMeResponse getMyInfo(Long userId) {
        User user = findActiveUser(userId);
        PointAccount pointAccount = pointAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_ACCOUNT_NOT_FOUND));

        return UserMeResponse.from(user, pointAccount.getBalance());
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
